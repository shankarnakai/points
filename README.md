# Scala REST API EXERCISE
This project exercises the creation of a REST api build with HTTP4S, Cats and DDD(Domain-Driven Design) concepts.

## Table of Contents
- [Stack Tech](#stacktech)
- [What does it do?](#what-does-it-do?)
- [Run](#run)
- [Dockerize](#dockerize)
- [Postman](#postman)
- [API](#api)

## StackTech 
- Scala 2.13 (Java 11)
- Http4s as the web server
- Circe for json serialization
- Cats for FP awesomeness
- ScalaTest for Unit test

## What does it do? 
The application will manage the point for user in their accounts.
Users are able to see a single balance in there account, but the system needs to keep track of their point per payer/partner.

In the application, each transaction record contains: payer name(string), points (integer), transactionDate (Date).

For earning points it is easy to assign a payer, the system know which actions earned the points
And thus which partner should be paying for the points.

There are two rules for determining what points to "spend" first:
We want the oldest points to be spent first
We want no payer's points to go negative.

## Run
You can run the project from the root of the project with the following command:
```shell
sbt run
```

## Dockerize 
To dockerize the application you can run:
```shell
sbt assembly
```
and then build the image
```shell
docker build --tag point:1.0 . 
```
You can run the image with
```shell
docker run --publish 8080:8080 --name points point:1.0
```

## API
Swagger API: [https://app.swaggerhub.com/apis-docs/shankarnakai/Points/1.0.0](https://app.swaggerhub.com/apis-docs/shankarnakai/Points/1.0.0)


## Postman
There is a file that has already all the endpoints available in the API and with a simple
test scenario that you can run to see how the points will be added and deducted
Import in your postman: [Points.postman_collection.json](./Points.postman_collection.json)

## TODO
- Use Tapir to describe api and auto-generate OpenAPI Specification
- Run swagger web page locally
- Improve endpoint response models
- Add ScalaCheck for Property-based testing
