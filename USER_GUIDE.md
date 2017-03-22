# Document Service Backup Restore Tool

Document Service Backup/Restore Tool, or simply `dbr`, allows to backup data from the Document Service 
and restore data to the Document service. Backup operation reads your documents from the Document service
using the Document Backup service and writes them as extended JSONs to files in a designated directory. 
This directory can be later used for restore process.
  
Both operations have an option to choose an environment. `dbr` supports all available environments 
(__us-prod__, __us-stage__, __eu__). With this, it is possible to migrate data from one environment to another.
     
Documents are being inserted as they are written in the backup files with all metadata and data types preserved. 

## Usage

After unzipping the tool, you should get the following directory structure: 

```
.
├── README.md
├── bin
│   ├── dbr
│   └── dbr.bat
└── lib
    ├── ...
```

In the following steps you'll be using `bin/dbr` tool to backup and restore.
In case you get `permission denied: bin/dbr`, just `chmod +x bin/dbr`. 

### Backup

The backup reads documents from the Document Backup service and stores them in files on the local file system.

``` bash
$ bin/dbr backup --env <env> --client <client> --config <config_file> --out <destination_dir>
```

Parameters: 
 
-	`env` - Name of an environment, possible values: us-prod, us-stage, eu.
-	`client` - Name of the client for whom the operation is performed.
-	`config` - File you have to create manually before the backup. It contains configuration where tenants and types are listed. For more information on the structure of config file, see the next section.
-	`out` - Destination directory where output files will be stored. Every backup operation creates a new directory in `out` directory with the following naming convention: `backup-TIMESTAMP`.
- 	`skipIndexes` - Skips backup of indexes

Backup requires `CLIENT_ID` and `CLIENT_SECRET` environment variable to be set with appropriate auth credentials for getting access token.

Example:

``` bash
$ export CLIENT_ID=<setme>
$ export CLIENT_SECRET=<setme>

$ bin/dbr backup --env us-prod  --client hybris.product --config config.json --out tmp/hybris_product_backup
```

#### Configuration file

The configuration file for backup contains a list of tenants to be downloaded. You have to create it manually before backup. 
Additionally you can specify which types should be downloaded. If types are not provided, then all of them will be included.


``` json
{
	"tenants" : [
		{
			"tenant" : "marketplace", "types" : ["products", "variants"]
		},
		{
			"tenant" : "marketdemo" 
		}
	]
}
```

#### Indexes

Along with backup of data, backup of indexes is done. To skip backup of indexes use `--skipIndexes`.

#### Outcome 

The outcome of backup is a set of files in the destination directory. Every type is stored in a separate file as an array of extended JSONs. 
The main file `backup.json` is a backup summary generated automatically during backup with details about the performed operations.  

### Restore

The restore operation imports data from files into the Document Backup service. The input for this operation is 
the backup's destination directory which includes the configuration file called `backup.json` (it's backup's summary file) 
as well as files with data in form of `UUID.json`. 
If you want to restore only a part of the data (e.g. some selected types), you can limit the input by editing the `backup.json`. 
More information on the `backup.json` file can be found in the Configuration section.

``` bash
$ bin/dbr restore --env <env> --dir <source_dir>
```

Parameters: 
 
-	`env` - Name of an environment, possible values: us-prod, us-stage, eu.
-	`dir` - Source directory with files containing the data. 
- 	`skipIndexes` - Skips restore of indexes

Restore requires `CLIENT_ID` and `CLIENT_SECRET` environment variable to be set with appropriate auth credentials for getting access token.

Example:

``` bash
$ export CLIENT_ID=<setme>
$ export CLIENT_SECRET=<setme>

$ bin/dbr restore --env us-prod --dir tmp/hybris_product_backup/backup-1488551648
```

#### Indexes

Even if backup was done with indexes, you can skip restore of indexes. Similar to backup, to skip restore of indexes use `--skipIndexes`.

#### Configuration

The configuration for restore process contains a list of types to be imported into the Document Backup. 
Each type's configuration holds an information about a client, tenant, type name and file name where the documents are stored.
You can manipulate this file in order to selectively restore selected types.

``` json
[
    {
        "client" : "hybris.product",
        "tenant" : "marketplace",
        "type" : "products",
        "file" : "388027ed-bdb2-43d7-8b21-387eba1dbd1f.json"
    },
    {
        "client" : "hybris.product",
        "tenant" : "marketplace",
        "type" : "variants",
        "file" : "6ab630cd-0404-476b-bc99-89271c5b0792.json"
        "indexes" : [
            {
                "keys" : {
                    "name" : 1
                },
                "options" : {
                    "name" : "variant-name-index"
                }
            }
        ]
        
    }
]
````
