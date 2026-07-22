[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/ClN7Iqxk)
# Assignment 1: DSLab SMQP Client

## Table of Contents

- [What is DSLab SMQP Client](#what-is-dslab-smqp-client)
- [GitHub Classroom Grading](#github-classroom-grading)
- [Protect Files and Directories](#protect-files-and-directories)
- [Assignment Structure](#assignment-structure)
- [Starting the Client Application](#starting-the-client-application)
- [Local Testing of the Client Application](#local-testing-of-the-client-application)

## What is DSLab SMQP Client?

DSLab SMQP Client is a simple java based client application, which is used to send and receive messages from a message
broker.
A command line interface is provided to the user to allow easy interaction with the broker. The client is designed to be
used with the corresponding message broker (DSLab Message Broker). The underlying protocol implemented by the client for
broker
communication is SMQP (Simple Message Queuing Protocol) which is heavily inspired by the broadly used AMQP (Advanced
Message Queuing
Protocol).

## GitHub Classroom Grading

To grade this assignment, we will use GitHub Actions to automatically build and test the code submitted by students.
After pushing your
solution to the GitHub repository, the GitHub Actions will run automatically and provide feedback on the correctness of
the solution.
The grading feedback is provided at the end of the execution of the GitHub Actions Workflow. The feedback will contain
information about
each test case and whether it passed successfully or failed. If a test case failed, you will be able to see the error
output in the corresponding
execution step.

### Protect Files and Directories

The following files and directories are protected and should not be modified by students since this will flag the
submission with a `warning`:

- `.github/**/*` which is used for the GitHub Actions Workflows (e.g. Classroom Autograding)
- `src/main/resources/**/*` which contains all the configuration files for this assignment
- `src/main/test/**/*` which contains all the tests and further helper classes for this assignment
- `pom.xml` which defines all necessary external dependencies for this assignment and is used to build and test the
  project with GitHub Actions

### Assignment Structure

The structure of the assignment is as follows:

- `src/main/java/**/*` contains all the source code for this assignment
- `src/main/test/**/*` contains all the tests and further helper classes for this assignment

### Starting the Client Application

To start the client application, you can use the following command:

```bash
# First compile the project with Maven
mvn compile
# Start the client application with the following command where componentId is one of client-0, client-1 or client-2.
mvn exec:java@<componentId>
# You can also combine both commands into one
mvn compile exec:java@<componentId>
```

### Local Testing of the Client Application

You can test the client application locally by using the following commands:

```bash
# To execute all tests you can use the following command:
mvn test
# To execute a single test method you can use the following command:
mvn test -Dtest="<testClassName>#<testMethodName>"
# E.g., to execute the test method displays_correct_prompt from the class ClientCLITest you can use the following command:
mvn test -Dtest="ClientCLITest#displays_correct_prompt"
```

