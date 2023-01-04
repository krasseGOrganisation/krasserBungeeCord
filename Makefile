dev:
	mvn clean
	mvn package
	scp ./bootstrap/target/BungeeCord.jar krasserHost:dev/bungeecord
	sh -c 'scp module/cmd-*/target/cmd_*.jar krasserHost:dev/bungeecord/modules'
	ssh krasserHost dev/bungeecord/restart.sh