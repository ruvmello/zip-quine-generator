# Recursive & Loopy Zip Generator
Author: Ruben Van Mello

Article: https://www.mdpi.com/2076-3417/14/21/9797

This project explores the creation of recursive and "loopy" zip files — unique types of zip files, which can be unzipped infinitly.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Details](#details)
- [Building a jar](#building-a-jar)
- [Contributing](#contributing)
- [License](#license)

## Introduction
Zip files are commonly used as a compression method to package files and folders. This project introduces two unique types of zip files:

 - Recursive Zip Files: Zip files that contain themselves endlessly. See the example [zipquine.zip](https://github.com/ruvmello/zip-quine-generator/tree/master/examples)
 - Loopy Zip Files: A novel approach where a zip file contains another zip file, which contains the original zip file, creating an infinite loop structure. See the example [Ouroboros.zip](https://github.com/ruvmello/zip-quine-generator/tree/master/examples)

This generator, written in Kotlin, provides an easy way to create both recursive and loopy zip files.

## Features
Recursive Zip Generation: Create a zip file that infinitly contains itself. The generator allows to add extra files inside this zip file as well, but it has its limitations. The extra files can't be bigger than 32,763 bytes including the generated headers.

Loopy Zip Generation: Generate zip files that have the following structure, zip1 -> zip2 -> zip1 -> ... They can also include extra files. The generator allows zip1 and zip2 to each contain one extra file, each file can't be bigger than 16,376 bytes including generated headers.

## Installation
### Prerequisites
JDK (Java Development Kit, version 17 or higher)

### Usage
Run the help command as follows:
  ```
  $ java -jar zip_quine_generator.jar --help

  This program aims to create a zip quine.
  The created zip contains the input file, as well as the zip itself.
  Usage: java -jar zip_quine_generator.jar inputFile [-o outputFile (ignored when using loop)] [-h] [--debug] [--no-crc] [--num-threads number_of_threads] [--loop]
  ```

As an example: if you want to generate a normal zip quine including two extra files one.txt and two.jpg, run the following.
  ```
  $ java -jar zip_quine_generator.jar one.txt two.jpg -o quine.zip
  ```
This will create a zip quine with the name "quine.zip". The resulting zip file will contain three files: one.txt, two.jpg and quine.zip itself.

If you want to create a loopy zip file, the program expects two input files and the --loop option.
  ```
  $ java -jar zip_quine_generator.jar one.txt two.jpg --loop
  ```
The output will be one.zip, which contains two.jpg and two.zip. In turn, two.zip will contain one.txt and the originally created one.zip. This process goes on infinitly.

**Note:** There is an option --no-crc. You might notice some zip files take really long to generate. The reason is the CRC-32 value needs to be stored in the zip headers of the zip file. 
Since the created zip file contains itself, we need to find a CRC-32 value that if we fill it in, it matches with the value of the whole file. This is a bruteforce process and can take some time. 
Some unzip tools don't require the CRC-32 to be correct, so it can be disabled. The --num-threads option specifies how many threads are used to bruteforce the CRC-32 value.

## Details
The creation of these types of zip files is not straight forward. This project was created as my master's thesis and builds upon the foundation of Russ Cox and Erling Ellingsen.
Before my thesis, there were only a handfull examples to find of normal recursive zip files. My thesis started as a task to create a generator for these files, since they are so rare.
As an extension, I explored the the possibility of loopy zip files, which were not found previously. We believe this is the first time this was done and it came with a lot of challenges.
Because of this achievement, my supervisor suggested to publish this as an article. The full explanation of how these files were created can be read in the journal: https://www.mdpi.com/2076-3417/14/21/9797

## Building a jar
You can build the jar using maven;
```
mvn package -DskipTests
```

## Contributing
1. Fork the repository.
2. Create a new branch (git checkout -b feature/YourFeature).
3. Commit your changes (git commit -am 'Add new feature').
4. Push to the branch (git push origin feature/YourFeature).
5. Create a new Pull Request.

## Contributors
Massive thanks to the contributors of the project. You deserve a special mention here from the bottom of my heart:
- NateChoe1

## License
This project is licensed under the MIT License. See the LICENSE file for details.

