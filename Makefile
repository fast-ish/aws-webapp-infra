.PHONY: help
help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-20s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.PHONY: install
install: ## Install dependencies
	mvn clean install -DskipTests

.PHONY: compile
compile: ## Compile the project
	mvn clean compile

.PHONY: test
test: ## Run unit tests
	mvn test

.PHONY: integration-test
integration-test: ## Run integration tests
	mvn verify -Pintegration

.PHONY: coverage
coverage: ## Generate code coverage report
	mvn clean test jacoco:report
	@echo "Coverage report available at: target/site/jacoco/index.html"

.PHONY: quality
quality: ## Run all quality checks
	mvn clean verify spotbugs:check pmd:check checkstyle:check

.PHONY: spotbugs
spotbugs: ## Run SpotBugs analysis
	mvn clean compile spotbugs:check

.PHONY: pmd
pmd: ## Run PMD analysis
	mvn clean compile pmd:check pmd:cpd-check

.PHONY: checkstyle
checkstyle: ## Run Checkstyle analysis
	mvn clean compile checkstyle:check

.PHONY: security
security: ## Run security vulnerability scan
	mvn dependency-check:check

.PHONY: sonar
sonar: ## Run SonarCloud analysis
	mvn clean verify sonar:sonar

.PHONY: format
format: ## Format code using Spotless
	mvn spotless:apply

.PHONY: format-check
format-check: ## Check code formatting
	mvn spotless:check

.PHONY: clean
clean: ## Clean build artifacts
	mvn clean
	rm -rf cdk.out

.PHONY: build
build: ## Build the project
	mvn clean package

.PHONY: deploy
deploy: ## Deploy to Maven repository
	mvn clean deploy

.PHONY: release
release: ## Create a release
	mvn release:prepare release:perform

.PHONY: validate
validate: ## Validate CDK templates with cfn-lint
	@if [ -f scripts/validate-templates.sh ]; then \
		scripts/validate-templates.sh; \
	else \
		echo "Error: scripts/validate-templates.sh not found"; \
		exit 1; \
	fi

.PHONY: update-deps
update-deps: ## Update Maven dependencies
	mvn versions:use-latest-versions

.PHONY: check-deps
check-deps: ## Check for dependency updates
	mvn versions:display-dependency-updates

.PHONY: docker-build
docker-build: ## Build Docker image
	docker build -t cdk-common:latest .

.PHONY: docker-run
docker-run: ## Run Docker container
	docker run -it --rm cdk-common:latest

.PHONY: lint
lint: checkstyle pmd spotbugs ## Run all linters

.PHONY: verify
verify: ## Run full verification
	mvn clean verify

.PHONY: site
site: ## Generate project site
	mvn site

.PHONY: debug
debug: ## Run with debug output
	mvn clean compile -X

.PHONY: tree
tree: ## Display dependency tree
	mvn dependency:tree

.PHONY: analyze
analyze: ## Analyze dependencies
	mvn dependency:analyze

.PHONY: effective-pom
effective-pom: ## Show effective POM
	mvn help:effective-pom

.PHONY: pre-commit
pre-commit: ## Run pre-commit checks
	mvn clean test checkstyle:check spotbugs:check spotless:check

.PHONY: javadoc
javadoc: ## Generate Javadoc
	mvn javadoc:javadoc
	@echo "Javadoc available at: target/site/apidocs/index.html"

.PHONY: all
all: clean build test quality ## Run everything
