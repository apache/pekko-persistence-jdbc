#!/bin/bash
echo "==================  Help for SqlServer cli  ========================"
echo "================================================================="
docker exec -it sqlserver-test /opt/mssql-tools18/bin/sqlcmd -S localhost -U docker -P Str0ngPassword -d docker
