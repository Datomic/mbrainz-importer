# Datomic Cloud Mbrainz Importer

## Importing the Mbrainz Subset

Create a config/manifest.edn file based on
config/manifest.edn.example. You will need to set the map under the
`client-cfg` key to be the args you use for `d/client`. Then

    clojure -m datomic.mbrainz.importer config/manifest.edn

## ADVANCED: Changing the Batch Size

Don't do this unless you know what you are doing. Don't import
at different batch sizes into the same db.

    clojure -m datomic.mbrainz.importer.batch subsets {batch-size}




