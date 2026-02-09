# Project Liliput

This presentation consists of two parts:

## Code

Java project initialized with Gradle (with wrapper).

### Requirements
- Java 24 or higher

### Usage
```bash
cd code

# Build the project
./gradlew build

# Run the application
./gradlew run

# Run with Project Lilliput enabled and hashCode from memory address
./gradlew runWithLilliput

# Run without Project Lilliput but with hashCode from memory address
./gradlew runWithoutLilliput

# Run tests
./gradlew test
```

### Run Configurations

Two Gradle tasks are available to compare performance with and without Project Lilliput:

#### runWithLilliput
Runs with Project Lilliput enabled and hashCode from memory address:
- `-XX:+UnlockExperimentalVMOptions` - Unlocks experimental VM options
- `-XX:+UseCompactObjectHeaders` - Enables Project Lilliput compact object headers (reduces object header size from 128 bits to 64 bits)
- `-XX:hashCode=4` - Generates identity hashcode from memory address (experimental feature)

#### runWithoutLilliput
Runs without Project Lilliput but with hashCode from memory address:
- `-XX:+UnlockExperimentalVMOptions` - Unlocks experimental VM options
- `-XX:hashCode=4` - Generates identity hashcode from memory address (experimental feature)

This allows you to compare the memory footprint and performance with and without compact object headers.

## Slides

Presentation slides using reveal.js.

### Requirements
- Node.js and npm (for development)

### Usage
```bash
cd slides

# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

Open `slides/index.html` in a browser to view the presentation.

### Creating Custom Slides

Edit `slides/index.html` to create your presentation content.
