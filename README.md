# Datomic Cloud Mbrainz Importer

Import the mbrainz sample data set.

# Importing the Mbrainz Subset

Create a config/manifest.edn file based on
config/manifest.edn.example. You will need to set the map under the
`client-cfg` key to be the args you use for `d/client`. Then

    clojure -M -m datomic.mbrainz.importer config/manifest.edn

# Watch the Video

[![IMAGE ALT TEXT](https://img.youtube.com/vi/oOON--g1PyU/0.jpg)](https://www.youtube.com/watch?v=oOON--g1PyU "Simplifying ETL with Clojure and Datomic")

# ADVANCED: Changing the Batch Size

Don't do this unless you know what you are doing. Never import
at different batch sizes into the same db.

    clojure -M -m datomic.mbrainz.importer.batch subsets {batch-size}




