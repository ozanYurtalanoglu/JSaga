![logo](https://i.imgur.com/a9PTiUl.png)
# About

JSaga is a software library for creating orchestration based saga pattern in microservice architecture. You can easily create your saga by adding steps to the saga using JSaga. You can also handle compensation scenarios to reverse the data to their initial states in the microservices which are part of the saga. JSaga is avaliable for java programming language for now. You will need to setup a kafka broker for communication between your microservices tu use JSaga.


# Saga Pattern

In traditional monolithic architectures, transactions are often managed by the system's underlying relational database. These databases offer ACID transactions (Atomicity, Consistency, Isolation, Durability) which ensure that all operations within a transaction boundary are completed successfully, or none are.

In distributed systems, especially in a microservices architecture, achieving such properties is challenging. Operations may span multiple services, each possibly having its own database. The Saga Pattern is introduced to manage such long-running, distributed transactions.

A Saga is a sequence of transactions where each transaction updates data within a single service and publishes an event to trigger the next local transaction in the Saga. If a local transaction fails, the saga executes compensating transactions to undo the impact of the preceding transactions.

There are two primary ways to coordinate sagas:

1. **Choreography**: Every service involved in the saga executes its local transaction and then publishes an event. Other services listen to events and execute the next transaction in line. There's no central coordination.
  
2. **Orchestration**: One service (or orchestrator) is responsible for central coordination. It tells the involved services what transaction to execute, and they respond when they're done.

# Example Scenario: Online Travel System

Imagine an online system where a customer can book a flight, a hotel, and rent a car all in one place. 

Microservices:
1. **Flight Service**: Books a flight.
2. **Hotel Service**: Reserves a room.
3. **Car Rental Service**: Rents a car.


**Possible Scenarios**:

1. **All Successful**: Flight booked > Room reserved > Car rented > Confirmation email sent.
2. **Hotel Booking Failure**: Flight booked > Room reservation fails. Here, the compensating action would be to cancel the flight booking.
3. **Car Rental Failure**: Flight booked > Room reserved > Car rental fails. The compensating actions would be to cancel both the flight  and room reservation.

Each service is responsible for its own part of the whole transaction. If the hotel service can't reserve a room, it will send an event indicating its failure.(which means compensation is needed) This will trigger the flight service to cancel the flight that was previously booked.

Each step in the saga represents a separate transaction. If any of these transactions fail, the saga ensures that compensating transactions are executed to maintain data consistency.

How JSaga Works

JSaga offers some classes to implement orchestration based saga pattern in ease. After you use these classes to implement your own saga, the saga you created works to communicate with other microservices over kafka broker. All you need to do is setup a kafka broker and do necessary configuration in your "build.gradle" and "application.properties" files. You can add jsaga.jar file to your build.gradle file after you download it.(For now I have been some trouble with mavencentral) Also you have to add some dependicies related to kafka. 


**build.gradle file**
    
    implementation files('/Users/ozan/Desktop/jsaga.jar')
    implementation 'org.apache.kafka:kafka-streams'
    implementation 'org.springframework.kafka:spring-kafka'

**application.properties file(localhost:9092 is an example address)**

    jsaga.kafka-address=localhost:9092



# Simple Explanations of JSaga Classes

**Saga<T,S>**: This class is fundamental class of JSaga library. You can use its "executeSaga" method to run your saga. 

**Saga.SagaBuilder<T,S>**: This class is builder class of Saga instances. You should use this class to create your saga.

**LocalActionDataHolder<T>**: This class is used for holding data after and before the local action which happens in the orchestrator microservice.(Basically the microservice you use JSaga in) you can use it on compensation state to rollback your transaction in the local microservice.

**SagaKafkaMessage** : This is used for communication between microservices which are part of the saga. 

**SagaKafkaMessageFactory** : This is used for creating saga messages between microservices. Its "createCommandWithContentAndTopicName" method is used for creating messages by topic and message content.

# Example Code 

Below you can see the code snippet for "Online Travel System". To remember, this system is where a customer can book a flight, a hotel, and rent a car all in one place. Flight microservice is our microservice which JSaga implemented as orchestrator.

Microservices:
1. **Flight Service**: Books a flight.
2. **Hotel Service**: Reserves a room.
3. **Car Rental Service**: Rents a car.

I prefer to create a factory class for putting all saga logic together although this factory class is not part of the JSaga library. Here is the code:


    @Component
    public class BookingSagaFactory {
    
    @Autowired
    private FlightDatabase flightDatabase;
    public Saga<BookingRequest, FlightEntity> makeBookingCreatorSaga(FlightRequest request) {
        Saga.SagaBuilder<BookingRequest, FlightEntity> sagaBuilder = new Saga.SagaBuilder<>();

        Saga<BookingRequest, BookingEntity> saga = sagaBuilder.addStartingStep()
            .setLocalAction(this::createFlight)
            .setLocalCompensationAction(this::rollbackFlightCreation)
            .setLocalActionInput(request)

            .addDispatcherStep()
            .setStepMessageGenerator(this::generateHotelReservationMessage)
            .setCompensationNeededChecker(this::checkCompensationByHotelServiceResponse)
            .setOnCompensationMessageGenerator
            (this::generateHotelReservationCompensationMessage)

            .addDispatcherStep()
            .setStepMessageGenerator(this::generateCarRentalMessage)
            .setCompensationNeededChecker(this::checkCompensationByCarServiceResponse)
            
            .buildSaga();

        return saga;
    }

    private LocalActionDataHolder<BookingEntity> createFlight(FlightRequest request) {
        
        FlightEntity flightCreated = flightDatabase.createFlight(request);
        LocalActionDataHolder<FlightEntity> localActionDataHolder = new LocalActionDataHolder<>();
        localActionDataHolder.setDataBeforeLocalAction(null);
        localActionDataHolder.setDataAfterLocalAction(flightCreated);
        return localActionDataHolder;
    }

    private void rollbackFlightCreation(LocalActionDataHolder<FlightEntity> localActionDataHolder) {
        Optional<FlightEntity> optionalFlightEntity = localActionDataHolder.getDataAfterLocalAction();
        FlightEntity flightEntity = optionalFlightEntity.get();
        flightDatabase.deleteFlight(flightEntity);
    }



    private SagaKafkaMessage generateHotelReservationMessage(LocalActionDataHolder<FlightEntity> localActionDataHolder) {
        SagaKafkaMessageFactory messageFactory = new SagaKafkaMessageFactory();
        String hotelMicroserviceCreateReservationSagaTopic = "create-reservation-saga";
        Optional<FlightEntity> optionalFlightEntity = localActionDataHolder.getDataAfterLocalAction();
        FlightEntity flightEntity = optionalFlightEntity.get();
        String customerName = flightEntity.getCustomerName();
        String messageString = "create reservation for "+customerName;
        SagaKafkaMessage message = messageFactory.createCommandWithContentAndTopicName(hotelMicroserviceCreateReservationSagaTopic,messageString);
        return message;
    }

    private boolean checkCompensationByHotelServiceResponse(SagaKafkaMessage responseMessage) {
        if(responseMessage.getMessageContent().equals("succesfully Reserved a hotel")){
        return true;
        }
        else{
        return false;
        }
    }

    private SagaKafkaMessage generateHotelReservationCompensationMessage(LocalActionDataHolder<FlightEntity> localActionDataHolder) {
        SagaKafkaMessageFactory messageFactory = new SagaKafkaMessageFactory();
        String hotelMicroserviceCreateReservationSagaTopic = "create-reservation-saga-compensation";
        Optional<FlightEntity> optionalFlightEntity = localActionDataHolder.getDataAfterLocalAction();
        FlightEntity flightEntity = optionalFlightEntity.get();
        String customerName = flightEntity.getCustomerName();
        String messageString = "delete reservation for "+customerName;
        SagaKafkaMessage message = messageFactory.createCommandWithContentAndTopicName(hotelMicroserviceCreateReservationSagaTopic,messageString);
        return message;
    }

    private SagaKafkaMessage generateCarRentalMessage(LocalActionDataHolder<FlightEntity> localActionDataHolder) {
        SagaKafkaMessageFactory messageFactory = new SagaKafkaMessageFactory();
        String hotelMicroserviceCreateReservationSagaTopic = "create-car-rental-saga";
        Optional<FlightEntity> optionalFlightEntity = localActionDataHolder.getDataAfterLocalAction();
        FlightEntity flightEntity = optionalFlightEntity.get();
        String customerName = flightEntity.getCustomerName();
        String messageString = "rent a car for "+customerName;
        SagaKafkaMessage message = messageFactory.createCommandWithContentAndTopicName(hotelMicroserviceCreateReservationSagaTopic,messageString);
        return message;
    }

    private boolean checkCompensationByCarServiceResponse(SagaKafkaMessage responseMessage) {
        if(responseMessage.getMessageContent().equals("succesfully rented a car")){
        return true;
        }
        else{
        return false;
        }
    }
    }



As you can see JSaga library uses "functional programming paradigm" for creating the saga. You can pass your methods as arguments to saga builder's methods. First you need to create a Saga.SagaBuilder instance. Then you can start to build your saga. First, you need to call "addStartingStep" method to setup the local actions. Now we can look at the other other parts of the code:

**setLocalAction(this::createFlight)**: This basically sets the local action of saga which must be taken in the orchestrator.(we can call it local microservice) "createFlight" method is which this action happens in. In this method related database transaction(creating flight) is done. Then a LocalActionDataHolder<FlightEntity> class' instance returns. This intance holds the data before the action(there is no data before the action so it is set to null) and after the action(created flight entity) As you can see "createFlight" method takes a intance of FlightRequest as it is determined in the Saga.SagaBuilder<T,S> class' first generic type "T". This instance is setted in the "setLocalActionInput" method. Also the the returned instance of the LocalActionDataHolder's generic type is FlightEntity as it is determined in the Saga.SagaBuilder<T,S> class' second generic type "S". This data holder object is "passed" the other methods like "generateHotelReservationCompensationMessage".

**setLocalCompensationAction(this::rollbackFlightCreation)**: This basically sets the local compensation action of saga which must be taken in the orchestrator. "rollbackFlightCreation" method is which this action happens in. In this method related rollback is done. For example if the second step of saga fails, Then this method is called and the flight entity which is created is deleted since the saga's all steps are not done successfully.

  After building the starting step, we can add dispatcher steps which manage communication between microservices that are parts of the saga. so we need to call "addDispatcherStep". Then we can build our dispatcher step. Now we can look at the remaining parts of the code:

**setStepMessageGenerator(this::generateHotelReservationMessage)**: This basically sets the method which generates kafka message sent to hotel microservice. "generateHotelReservationMessage" method returns SagaKafkaMessage class' instance. As you can see this is created by SagaKafkaMessageFactory.

**setCompensationNeededChecker(this::checkCompensationByHotelServiceResponse)**: This basically sets the method which checks the message returned from hotel microservice to determine if the compensation is needed. After "hotel microservice" takes the saga command message, it is supposed to send back reply message to flight microservice.(orchestrator) checkCompensationByHotelServiceResponse control this reply message to determine whether the compensation is needed. If it is needed, the method returns true and local compensation action triggered.(rollbacking the flight creation) You can ask why the topic which the reply message expected is not determined. Because the reply message is always waited from the topic whose name includes the name of microservice topic which is already determined followed by "-response" at the end. For example the hotel microservice command message topic determined as "create-reservation-saga" so the reply message must be anticipated on topic named "create-reservation-saga-response"

**setOnCompensationMessageGenerator(this::generateHotelReservationCompensationMessage)**: This basically sets the method which generates the message for hotel microservices on compensation state. But it can be tricky because this compensation message is not called when hotel microservice fails. Because if the hotel microservice fails, there is no need to send any compensation message to hotel microservice since it has not done anything yet. So the method is called when the car rental microservice fails.

  The other method calls like "setStepMessageGenerator(this::generateCarRentalMessage)" work as expected. For example if "checkCompensationByCarServiceResponse" method returns true, then the saga enters compensation status which requires all compensation related steps are done in order.

  After you created your saga with saga builder, you can start your saga by calling "executeSaga" method. this methods returns "Boolean" instance which means whether the saga executed without compensation. So if the method returns false, this means in a step the saga has entered compensation status.

# What Happens If a Compensation Message Couldn't Be Sent

If a compensation message can not be sent in a step, saga continues to execute compensation related methods. This can happen for any exception reason or if the compensation message could not reach the kafka broker in 30 seconds for one reason(broker could be down etc.). This situation means that one or more compensation message did not reach the destination microservice so these must reach after saga execution at least.(otherwise the other microservices can hold false data in them) So JSaga offers "SagaExecutionFailure" class to handle this issue. After you execute the saga, you can call "getOnCompensationDispatcherStepFailureList()" then you can solve the problem like the code below:

    List<SagaExecutionFailure<Order>> sagaExecutionFailureList = saga.getOnCompensationDispatcherStepFailureList();
    SagaKafkaMessage sagaKafkaMessage = sagaExecutionFailureList.get(0).getOnCompensationKafkaMesssage();
    sagaExecutionFailureList.get(0).solveCompensationFailure(sagaKafkaMessage);

Here the generic type "Order" the same with generic type S in the class Saga<T,S> 

You can also reach your LocalActionDataHolder instance in that step like the code below: 

    sagaExecutionFailureList.get(0).getLocalActionDataHolderWhenCompensationStepFailed();

Then you can check if the message is taken by kafka broker like the code below:
    
    sagaExecutionFailureList.get(0).isFailureSolved();

# Important Notes

-If the anticipated response does not come from the current communicating microservice after saga command message is sent in 10 seconds, then the saga enters compensation status. You can change the time span by adding the config below to application.properties file.

example config:
jsaga.command-message-response-timeout-seconds=5

-If any exception is thrown in any dispatcher step's method(like compensation checker) the saga enters the compensation status. 




      




