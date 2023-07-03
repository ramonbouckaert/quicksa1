# QuickSA1
[Statistical Areas Level 1 (SA1s)](https://www.abs.gov.au/statistics/standards/australian-statistical-geography-standard-asgs-edition-3/latest-release) are geographical areas used by the Australian Bureau of Statistics. SA1s divide Australia up neatly into over 50,000 regions, each with a population of between 200 and 800. The ABS uses these regions for analyisis and visualisation of data, however, these boundaries have another convenient use: they neatly divide Australian electorates up into geographic units for political canvassing activities such as doorknocking. As a general rule, one suburban SA1 represents one day's doorknocking for one person.

QuickSA1 is an easy-to-use tool for rapidly generating printable PDF maps of SA1s on the fly. If set up as an AWS Lambda function, it can support an API for generating shareable links for these PDFs.

QuickSA1 is written in Kotlin, and expects a Postgresql database with the PostGIS extension installed. When built, QuickSA1 produces two executable JARs: a command-line tool for setting up QuickSA1, and a JAR that can be used as an AWS Lambda function.

A hosted version of this tool can be used at [https://www.quicksa1.com/{INSERT_SA1_HERE}](https://www.quicksa1.com/80106106801)

## Build
To build all modules, run:

`./gradlew build`

This will produce `build` folders for each of the modules: `lambda`, `shared` and `utility`.
- `shared/build/libs/shared.jar` contains shared classes used by both the Lambda and the Utility tool
- `lambda/build/libs/lambda-all.jar` is a shaded JAR that can be used as an AWS Lambda function
- `utility/builds/libs/utility-all.jar` is a shaded executable JAR that provides a command-line utility for setting up, populating, and testing a QuickSA1 database.

The build targets Java 17.

## Run
To use the utility tool, run:

`java -jar utility/build/libs/utility-all.jar`

The tool will prompt you for:
- a JDBC connection string to your PostGIS database (starting with `jdbc:postgresql_postGIS://`)
- an option to update the database schema to create the tables expected by QuickSA1
- an option to populate the database with data from the Australian Bureau of Statistics
- the URL for the ABS's "Main Structure & Greater Capital City Statistical Areas" GeoPackage in ZIP format. This can be found on the [ABS Website ASGS Release]((https://www.abs.gov.au/statistics/standards/australian-statistical-geography-standard-asgs-edition-3/latest-release)) under *Access and Downloads* → *Digital boundary files* → *Downloads for GDS2020 digital boundary files*
- an option to produce a test PDF

To use the lambda, configure an AWS Lambda function:
- select Java 17 for the language
- set the handler reference to `io.bouckaert.quicksa1.lambda.ServePDF::handleRequest`
- create an environment variable called `DB_URL` and set it to your JDBC connection URL (starting with `jdbc:postgresql_postGIS://`)
- create an environment variable called `DB_DRIVER` and set it to your JDBC driver, it will probably be `net.postgis.jdbc.DriverWrapper`

You can test the lambda by using the following event JSON:

```json
{
  "pathParameters": {
    "sa1": "80106106801"
  }
}
```

You should receive back a JSON object that has a `body` property that contains a Base64-encoded binary PDF file.

## TODO
- I want the maps to be entirely vector based, no pixelated raster underlay.
- I want blocks and house number on the map. I will probably add blocks in the data model and write ingestors for each state planning authority. This might remove the need for an underlay altogether.
- It needs to be faster! It's a bit too slow right now to be worthy of the name QuickSA1
- I want to be able to enter an SA2 code and generate a multi-page PDF of every SA1 within that SA2.
- I want to be able to request SA1s by their human-readable name (e.g. `Suburb 5`) as well as by their code.
- I want a web interface of some kind so you can find and choose the SA1 to generate, rather than having to know the code.