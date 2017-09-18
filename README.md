# Datomic Cloud Mbrainz Importer

## Importing the Mbrainz Subset

    clojure -m datomic.mbrainz.importer {manifest-file}

Example

    clojure -m datomic.mbrainz.importer examples/manifest.edn

## Changing the Batch Size

Don't do this unless you know what you are doing. Don't import
at different batch sizes into the same db.

    clojure -m datomic.mbrainz.importer.batch subsets {batch-size}




