#!/bin/bash
echo "==================   Help for mysql cli  ========================="
echo "================================================================="
docker exec -it mysql-test mysql --user=root --password=root pekko_persistence_jdbc
