clean_compile:
	@clear
	@echo "Cleaning and compiling..."
	@mvn clean compile
	@echo "Done.\n\n"

package: clean_compile
	@echo "Packaging the jar..."
	@mvn package
	@echo "Done.\n\n"

.PHONY: clean_compile package
