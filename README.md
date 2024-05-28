
# Acn Outbox Starter

`acn-outbox-starter` is a Spring Boot starter that implements the outbox pattern to ensure reliable event processing in distributed systems. It is agnostic to the specific event handling logic, leaving the responsibility to the client code to manage the actual record operation as well as handling the events that have been recorded in the outbox.

## Table of Contents

- [Installation](#installation)
    - [Prerequisites](#prerequisites)
- [Usage](#usage)
    - [Implementing OutboxItemHandlerProvider](#implementing-outboxitemhandlerprovider)
    - [Using OutboxEventRecorder](#using-outboxeventrecorder)
- [Features](#features)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Installation

To use `acn-outbox-starter`, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>it.gov.acn</groupId>
    <artifactId>acn-outbox-starter</artifactId>
    <version>?.?.?</version>
</dependency>
```

### Prerequisites

- **Database**: Ensure your database has the required `transactional_outbox` table:

```sql
CREATE TABLE IF NOT EXISTS transactional_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_attempt_date TIMESTAMP WITH TIME ZONE,
    completion_date TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL,
    event TEXT NOT NULL,
    last_error TEXT
);
```

- **Shedlock**: This starter requires Shedlock for handling locks, so ensure the Shedlock table is created as [prescribed by Shedlock](https://github.com/lukas-krecan/ShedLock?tab=readme-ov-file#jdbctemplate).

## Usage

There are only two things the client app needs to do:

1. Implement the `OutboxItemHandlerProvider` interface to handle the outbox items. The outbox items are those that are retrieved at each outbox run, according to the configuration provided.
2. Use the `OutboxEventRecorder` bean to record events into the outbox.

### Implementing OutboxItemHandlerProvider

Create a class that implements the `OutboxItemHandlerProvider` interface and provides the logic for handling the outbox items.

```java
import org.springframework.stereotype.Component;

@Component
public class MyOutboxItemHandlerProvider implements OutboxItemHandlerProvider {
    @Override
    public void handle(OutboxItem outboxItem) {
        // Implement your message sending logic here
    }
}
```
__Important__: Ensure that any exceptions encountered during event handling are thrown or rethrown so that the outbox knows the sending didn't succeed and should be retried in future runs.


### Using OutboxEventRecorder

Inject the `OutboxEventRecorder` bean and use it to record events into the outbox.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyBusinessService {
  private final OutboxEventRecorder outboxEventRecorder;

  @Autowired
  public MyBusinessService(OutboxEventRecorder outboxEventRecorder) {
    this.outboxEventRecorder = outboxEventRecorder;
  }

  public void createSomething() {
    MyType something;
    // Perform your business logic here
    // ...
    // At the appropriate moment, record the event in the outbox
    outboxEventRecorder.recordEvent(something, "SomethingCreatedEvent");
  }
}

```







## Features

- Implements the outbox pattern for reliable event processing.
- Supports custom event handling through `OutboxItemHandlerProvider`.
- Configurable scheduling for event processing with retry logic.
- Integrates with Shedlock for distributed lock management.

## Configuration

Configure the outbox scheduler and other properties in your `application.yml`:

```yaml
acn:
  outbox:
    scheduler:
      enabled: true
      table-name: transactional_outbox
      fixed-delay: 60000 # milliseconds
      max-attempts: 3
      backoff-base: 5    # minutes
```

### Configuration Properties

| Property                          | Description                                                                                                      | Default Value          |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------|------------------------|
| `acn.outbox.scheduler.enabled`    | Enable or disable the outbox scheduler.                                                                          | `false`                |
| `acn.outbox.scheduler.table-name` | The name of the database table used for the outbox.                                                              | `transactional_outbox` |
| `acn.outbox.scheduler.fixed-delay`| Fixed delay in milliseconds between the end of the last invocation and the start of the next.                    | `60000` (60 seconds)   |
| `acn.outbox.scheduler.max-attempts`| The maximum number of attempts to process an item from the outbox, after which it will no longer be processed.   | `3`                    |
| `acn.outbox.scheduler.backoff-base`| The base value for the backoff calculation in minutes. See the example below for how backoff is calculated.      | `5` (minutes)          |

#### Backoff Calculation Example

With `backoff-base=5` and `max-attempts=4`:
- **First attempt**: as soon as the scheduler runs.
- **Second attempt**: 5 minutes after the first failed attempt.
- **Third attempt**: 25 minutes after the second failed attempt.
- **Fourth attempt**: 125 minutes after the third failed attempt.
```

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request for review.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

For any inquiries or support, please contact [your-email@example.com].
