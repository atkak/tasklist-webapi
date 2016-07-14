# Task List Web API

This is a portfolio project and its Web API part.

## Build and publish container

```
$ activator dockerBuild
$ docker login
$ docker push xiongmaomaomao/tasklist-webapi
```

## Run in local

Using default profile aws credentials.

```
$ docker run --rm -v ~/.aws:/root/.aws -p 9000:9000 xiongmaomaomao/tasklist-webapi
```

## Deploy

```
// create the artifact for upload to Elastic Beanstalk
$ activator ebBuild
$ eb deploy
```
