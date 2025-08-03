# nanoid-java

Java port of the [nanoid](https://github.com/ai/nanoid) unique string ID generator.

A tiny, secure, URL-friendly, unique string ID generator for Java.

## Features

- **Small.** Just 1 class.
- **Safe.** It uses hardware random generator. Can be used in clusters.
- **Short IDs.** It uses a larger alphabet than UUID (A-Za-z0-9_-). So ID size was reduced from 36 to 21 symbols.
- **Portable.** Nano ID was ported to multiple programming languages.

## Installation

### Maven

```xml
<dependency>
    <groupId>de.albahrani.nanoid</groupId>
    <artifactId>nanoid-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```gradle
dependencies {
    implementation 'de.albahrani.nanoid:nanoid-java:1.1.0'
}
```

**No authentication required!** This library is published to Maven Central for easy consumption.

## Usage

### Default ID (21 characters)

```java
import de.albahrani.nanoid.NanoId;

String id = NanoId.nanoid();
// Result: "K5urO4VjCU0LJEJmSGy0v"
```

### Custom Length

```java
String id = NanoId.nanoid(10);
// Result: "jvqVeJJhBJ"
```

### Custom Alphabet

```java
String id = NanoId.customNanoid("ABCDEF", 10);
// Result: "FBAEFAADEB"
```

## API

### `nanoid()`

Generate a URL-friendly unique ID. This method uses the URL-friendly alphabet `A-Za-z0-9_-` and returns an ID with 21 characters (to have a collision probability similar to UUID v4).

### `nanoid(int size)`

Generate a URL-friendly unique ID with custom length.

- `size` - ID length

### `customNanoid(String alphabet, int size)`

Generate unique ID with custom alphabet and length.

- `alphabet` - Alphabet to use for ID generation
- `size` - ID length

## Security

See [Nano ID collision calculator](https://zelark.github.io/nano-id-cc/) for collision probability.

## Comparison with UUID

Nano ID is quite comparable to UUID v4 (random-based). It has a similar number of random bits in the ID (126 in Nano ID and 122 in UUID), so it has a similar collision probability:

> For there to be a one in a billion chance of duplication, 103 trillion version 4 IDs must be generated.

There are three main differences between Nano ID and UUID v4:

1. Nano ID uses a bigger alphabet, so a similar number of random bits are packed in just 21 symbols instead of 36.
2. Nano ID code is smaller than `uuid/v4` package: 130 bytes instead of 423.
3. Because of memory allocation tricks, Nano ID is 16% faster than UUID.

## License

MIT License. See [LICENSE](LICENSE) for details.
