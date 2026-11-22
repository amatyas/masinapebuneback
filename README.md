# VisiologicBatch

## INSTALLATION SERVICES
``` bash

# Place DISTRIB
Place the distrib folder and its contents if missing
distrib folder must content :
1 - jre1.8.0_win folder and subfolders
2 - application-prod.yml
3 - logback-prod.xml
4 - startBatch.sh
5 - visiologic-batch-1.0.jar
ou copier le dossier distrib dans https://github.com/onelogic/VisiologicBatch/tree/master/distrib et le mettre dans le root /bin/Release au même niveau que fichier .exe

# PROD LOG
1 - Edit [ROOT]/distrib/logback-prod.xml
2 - Set Value of LOG_FOLDER  <property name="LOG_FOLDER" value="[FULL_Path]/[ROOT]/distrib/logs" />
3 - Verify url datasource in [ROOT]/distrib/application-prod.yml

# INSTALL SERVICE
1 - (Optionnal) Verify if some path or value changed in VisiologicBatchService.exe.config
2 - Install Install_Start_WindowsService.bat as Administrator
```
