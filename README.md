# MFS-BatchMapper-Generator

MFS BatchMapper Generator creates batchMap out of MFS PDFs
Output to the format of [ImageTester](https://github.com/applitools/ImageTester/)

## How It Works

The BatchMapper parses given MFS PDFs and reads the table of contents.
For each section a test is generated with that section's name and contains the relevant pages relative to the document.
In addition to the `map.csv` file the command to execute ImageTester with the map and batch is generated.

## Execution

>java -jar MFS-BatchMapper-Generator.jar

+ Optional parameters and flags:

  + `-cg,--cmdgen`              Generate Commands in addition to csv
  + `-f,--folder <path>`        Set the root folder to start the analysis, default: \.
  + `-fb,--flatbatch <name>`    Aggregate all test results in a single batch (aka flat-batch), default: "Report Testing"
  + `-k,--apiKey <apikey>`      Applitools Apikey
  + `-o,--offset <offset>`      Number of pages to offset for the page number count, default: TOC page number
  + `-s,--server <url>`         Set Applitools server url, default: https://mfseyes.applitools.com
  + `-sc,--section <section>`   Map only a specific section
  + `-t,--toc <toc>`            Table of Contents page number, default: 2
