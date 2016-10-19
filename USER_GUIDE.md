# Document Service Backup Restore Tool

Document service backup and restore tool, or simply `dbr`, allows to backup data from Document service 
and restore data to Document service. The backup operation reads documents from the Document service and 
writes them as JSONs to files in a designated directory. This directory can be later used for restoration.
  
Both operations have an option to choose an environment. `dbr` supports all available environments 
(__us-prod__, __us-stage__, __eu__). With this, it is possible to migrate data from one environment to another.
     
The restore operation uses Document service's _raw write_ feature. Documents are being inserted 
as they are written in the backup files, metadata information is not changed. 

## Usage

### Backup

The backup reads documents from the Document service and stores them into files into the local file system. 

```
$ dbr backup --env <env> --client <client> --config <config_file> --out <destination_dir>
```

parameters: 
 
-	`env` - name of an environment, possible values: us-prod, us-stage, eu
-	`client` - name of the client for whom the operation is performed
-	`config_file` - file with backup configuration where tenants and types are listed
-	`destination_dir` - destination folder where output files will be stored

Backup requires `CLIENT_ID` and `CLIENT_SECRET` environment variable to be set with appropriate auth credentials for getting access token.

Example:

```
$ export CLIENT_ID=<setme>
$ export CLIENT_SECRET=<setme>

$ dbr backup --env us-prod  --client hybris.product --config config.json --out /tmp/hybris_product_backup
```

#### Configuration file

The configuration file for backup contains a list of tenants to be downloaded.
Additionally we can specify which types should be downloaded. 
If types are not provided, then all types will be included.


```
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


#### Outcome 

The outcome of backup is a set of files in the destination directory. Every type is stored in a separate file as an array of JSONs. 
The main file `backup.json` is a backup summary with details about the performed operation.  

### Restore

The restore operation imports data from files into the Document service. The input for this operation is 
the backup's destination directory and the configuration file is the backup's summary file. 
If you want to restore only a part of the data, you can limit the input by editing the configuration file.

```
$ dbr restore --env <env> --client <client> --config <config_file> --dir <source_dir>
```

parameters: 
 
-	`env` - name of an environment, possible values: us-prod, us-stage, eu
-	`client` - name of the client for whom the operation is performed
-	`config_file` - file with restore configuration, contains a list of types to restore 
-	`source_dir` - source directory with files containing data 

Restore requires `CLIENT_ID` and `CLIENT_SECRET` environment variable to be set with appropriate auth credentials for getting access token.

Example:

```
$ export CLIENT_ID=<setme>
$ export CLIENT_SECRET=<setme>

$ dbr restore --env us-prod --client hybris.product --config /tmp/hybris_product_backup/backup.json --dir /tmp/hybris_product_backup
```

#### Configuration

The configuration for restoration contains a list of types to be imported into Document service. 
Each type's configuration holds information about a client, a tenant, a name of a type and a name of a file where documents are stored.

```
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
    }
]
````
