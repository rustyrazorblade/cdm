binary:
	rm -rf target/*jar-with-dependencies.jar
	rm -f bin/cdm
	mvn -e package
	mkdir -p bin
	cat src/main/sh/execute.sh target/cdm-0.11.jar > bin/cdm
	chmod 755 bin/cdm

