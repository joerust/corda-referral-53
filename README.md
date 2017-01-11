![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# CorDapp Template

Welcome to the CorDapp template. The CorDapp template is an example CorDapp 
which you can use to bootstrap your own CorDapp projects.

This README is an abridged version of 
the [CorDapp tutorial](http://docs.corda.net/tutorial-cordapp.html) found on
the Corda docsite.

**Instead, if you are interested in exploring the Corda codebase itself,
contributing to the core Corda platform or viewing and running sample
demos then clone the [corda repository](https://github.com/corda/corda).**

The code in the CorDapp template implements the _"Hello World"_ of
CorDapps. It allows users of a Corda node to generate and send referrals
orders to other nodes. You can also enumerate all the referrals
which have been agreed with other nodes. The nodes also provide a simple
web interface which can be used to interact with the CorDapp.

The source code for this CorDapp is provided in both Kotlin (under `/kotlin`)
and Java (under `/java`), and users can choose to write their CorDapps in
either language.

## The Example CorDapp

The Example CorDapp implements a basic scenario where a buyer wishes to
submit referrals to a partner. The scenario defines four nodes:

* **Controller** which hosts the network map service and validating notary
  service.
* **NodeA** who is the referer.
* **NodeB** who is the business potentially providing services.
* **NodeC** an unrelated third party.

NodeA can generate referrals and associated metadata such as delivery
address and delivery date. The flows used to facilitate the agreement 
process always result in an agreement with the partner busines as long
as the referral meets the contract constraints which are defined in 
`ReferralContract.kt`.

All referals between NodeA and NodeB become "shared facts"
between NodeA and NodeB. Note that NodeC won't see any of these
transactions or have copies of any of the resulting `ReferralState`
objects. This is because data is only propagated on a need-to-know
basis.

## Pre-Requisites

You will need the following installed on your machine before you can start:

* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
  installed and available on your path.
* Latest version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) 
  (note the community edition is free)
* [h2 web console](http://www.h2database.com/html/download.html)
  (download the "platform-independent zip")
* git

For more detailed information, see the
[pre-requisites](https://docs.corda.net/pre-requisites.html) page on the
Corda docsite.

## Getting Set Up

To get started, clone this repository with:

     git clone http://slgramihqgita91.info53.com/jrust1978/corda-template.git

Change directories to the newly cloned repo:

     cd cordapp-template
     
Now check out the latest stable milestone release:

     git checkout -b your-branch 
     
Instead, if you would like to build your CorDapp against a SNAPSHOT 
release of Corda then you can follow the instructions on the 
[CorDapp tutorial page](http://docs.corda.net/tutorial-cordapp.html) under 
the "Using a SNAPSHOT release" heading.
     
Build the CorDapp template:

**Unix:** 

     ./gradlew deployNodes
     
**Windows:**

     gradlew.bat deployNodes
     
Note. You will be building the example CorDapp. If you want to make any
changes they should be made before building, of course!
     
Gradle will grab all the dependencies for you from Maven and then build 
two sample applications and create several local Corda nodes.

## Open intellij

Open the intellij platform and import an existing project from this directory.

## Interacting with the CorDapp via HTTP

The CorDapp defines a couple of HTTP API end-points and also serves some
static web content. The end-points allow you to list agreements and add
agreements.

The nodes can be found using the following port numbers, defined in
`build.gradle` and the respective `node.conf` file for each node found
in `kotlin/build/nodes/NodeX` or `java/build/nodes/NodeX`:

     NodeA: localhost:10005
     NodeB: localhost:10007
     NodeC: localhost:10009

Also, as the nodes start-up they should tell you which host and port the
embedded web server is running on. The API endpoints served are as follows:

     /api/example/me
     /api/example/peers
     /api/example/referrals
     /api/example/{COUNTERPARTY}/create-purchase-order
     
The static web content is served from:

     /web/referral
     

