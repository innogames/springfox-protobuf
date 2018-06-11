# Springfox Protobuf Support
This module aims to add usable support to sue protobuf messages in springfox. It provides a jackson module that 
can be registered in SpringFox's `ObjectMapper`.

The provided module does not handle the actual (de-)serialization of messages. Use e.g. the 
[jackson-datatype-protobuf](https://github.com/HubSpot/jackson-datatype-protobuf) module for that functionality.

## Setup
Add this module and jackson-datatype-protobuf to your dependencies. 

Configure SpringFox's `ObjectMapper`. Also specify that your endpoints can produce json (either via e.g. 
`@GetMapping(produces="application/json")` or via your `Docket`).

```java
@Configuration
@EnableSwagger2
public class SpringFoxConfiguration implements ApplicationListener<ObjectMapperConfigured> {
	/** Configure SpringFox's internal ObjectMapper. */
	@Override
	public void onApplicationEvent(ObjectMapperConfigured event) {
		event.getObjectMapper().registerModule(new ProtobufModule());
		event.getObjectMapper().registerModule(new ProtobufPropertyModule());
	}
	
	/** Set up Docket for endpoints that produce protobuf. */
	@Bean
	public Docket myDocket() {
		return new Docket(DocumentationType.SWAGGER_2)
			// your normal configuration
			.produces(new HashSet<>(Arrays.asList("application/json", "application/x-protobuf")));
	}
}
```

## Usage
Open up springfox as usual. Protobuf endpoints should show up with their properties and be usable. Make sure to 
select `application/json` in the dropdown to get the human readable representation.
