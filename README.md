# Document Service Backup Tool

Document service backup tool (`dsb`) allows to backup data from Document service

## Usage

### Backup

```
$ dbr backup --env <env> --config <config_file> --client <client> --out <destination_dir>
```

parameters: 
 
-	`env` - name of an environment, possible values: us-prod, us-stage, eu
-	`config_file` - configuration file with detailed information for current operation
-	`client` - name of the client for whom the operation is performed
-	`destination_dir` - destination folder where output files will be stored

Backup requires `DOCUMENT_HTTP_CREDENTIALS` environment variable to be set with appropriate basic auth credential.

example:

```
$ export DOCUMENT_HTTP_CREDENTIALS=<setme>

$ dsb backup --env us-prod  --config config.json --client hybris.product --out /tmp/hybris_product_backup
```

#### Configuration file

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

In configuration file we define tenants whose date will be read to file.
Additionally we can specify which types should be downloaded. If types are not provided all types will be included.