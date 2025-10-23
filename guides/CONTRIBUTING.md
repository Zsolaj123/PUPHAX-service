# PUPHAX Service - Contributing Guide

Thank you for your interest in contributing to PUPHAX REST API Service!

## 🎯 Ways to Contribute

- 🐛 Report bugs and issues
- 💡 Suggest new features
- 📝 Improve documentation
- 🔧 Submit bug fixes
- ✨ Implement new features
- 🧪 Write tests
- 🌍 Add translations

---

## 📋 Before You Start

1. **Check existing issues** - Someone may already be working on it
2. **Discuss major changes** - Open an issue first to discuss your approach
3. **Follow conventions** - Match existing code style and patterns
4. **Write tests** - Include unit tests for new features
5. **Update documentation** - Keep docs in sync with code changes

---

## 🚀 Development Setup

### Prerequisites

- Java 17 (JDK)
- Maven 3.9+ or use included `./mvnw`
- Git
- Docker (optional, for testing)

### Clone and Setup

```bash
# 1. Fork the repository on GitHub
# 2. Clone your fork
git clone https://github.com/YOUR-USERNAME/PUPHAX-service.git
cd PUPHAX-service

# 3. Add upstream remote
git remote add upstream https://github.com/Zsolaj123/PUPHAX-service.git

# 4. Build project
./mvnw clean install

# 5. Run tests
./mvnw test

# 6. Run application
./mvnw spring-boot:run
```

Verify: `curl http://localhost:8081/api/v1/drugs/health/quick`

---

## 🏗️ Project Structure

```
PUPHAX-service/
├── src/
│   ├── main/
│   │   ├── java/com/puphax/
│   │   │   ├── config/         # Configuration classes
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── exception/      # Custom exceptions
│   │   │   ├── model/
│   │   │   │   └── dto/        # Data Transfer Objects
│   │   │   └── service/        # Business logic
│   │   └── resources/
│   │       ├── data/           # CSV fallback data
│   │       └── application.yml # Configuration
│   └── test/                   # Unit and integration tests
├── docker/                     # Docker configuration
├── docs/                       # Technical documentation
├── guides/                     # User guides
└── reference/                  # NEAK reference data
```

---

## 💻 Coding Guidelines

### Java Style

- **Code style:** Follow standard Java conventions
- **Naming:**
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Line length:** Max 120 characters
- **Imports:** No wildcard imports
- **Comments:** Javadoc for public APIs

### Example

```java
package com.puphax.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling drug-related operations.
 *
 * @author Your Name
 * @since 1.1.0
 */
@Service
public class DrugService {

    private static final Logger logger = LoggerFactory.getLogger(DrugService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Searches for drugs by name or active ingredient.
     *
     * @param searchTerm Search term (required)
     * @param page Page number (0-based)
     * @param size Page size (1-100)
     * @return DrugSearchResponse with paginated results
     * @throws IllegalArgumentException if searchTerm is null or empty
     */
    public DrugSearchResponse searchDrugs(String searchTerm, int page, int size) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty");
        }

        logger.debug("Searching drugs: term='{}', page={}, size={}", searchTerm, page, size);
        // Implementation...
    }
}
```

### Spring Boot Conventions

- Use `@Service`, `@Repository`, `@Controller` annotations
- Constructor injection over field injection
- Use `@Slf4j` from Lombok for logging (or manual logger)
- Validate input with `@Valid` and JSR-303 annotations

### Testing

- Unit tests for all services and utilities
- Integration tests for controllers
- Use JUnit 5 and Mockito
- Aim for >80% code coverage

**Test naming:**

```java
@Test
void searchDrugs_withValidTerm_returnsResults() {
    // Test implementation
}

@Test
void searchDrugs_withEmptyTerm_throwsException() {
    // Test implementation
}
```

---

## 🔄 Git Workflow

### Branching Strategy

- `main` - Stable production code
- `develop` - Development branch (if used)
- `feature/your-feature` - New features
- `bugfix/issue-number` - Bug fixes
- `hotfix/critical-issue` - Critical production fixes

### Creating a Branch

```bash
# Update your fork
git checkout main
git pull upstream main

# Create feature branch
git checkout -b feature/add-drug-categories
```

### Making Changes

```bash
# Make your changes
# Add files
git add src/main/java/com/puphax/service/NewService.java

# Commit with descriptive message
git commit -m "Add drug category filtering support

- Implement CategoryService for drug categorization
- Add category field to DrugSummary DTO
- Add /api/v1/drugs/categories endpoint
- Include unit tests for new functionality

Closes #42"
```

### Commit Message Format

```
<type>: <subject>

<body>

<footer>
```

**Types:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Build, dependencies, etc.

**Example:**

```
feat: Add drug price comparison endpoint

- Implement PriceComparisonService
- Add /api/v1/drugs/compare-prices endpoint
- Include price history from CSV data
- Add pagination support

Closes #123
```

### Pushing Changes

```bash
# Push to your fork
git push origin feature/add-drug-categories

# Create Pull Request on GitHub
```

---

## 🔍 Pull Request Process

### Before Submitting

1. **Test your changes:**
   ```bash
   ./mvnw clean test
   ./mvnw clean package
   ./mvnw spring-boot:run
   ```

2. **Check code style:**
   ```bash
   ./mvnw checkstyle:check  # If configured
   ```

3. **Update documentation** if needed

4. **Rebase on latest main:**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

### PR Template

When creating a PR, include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
How has this been tested?

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-reviewed my own code
- [ ] Commented code where necessary
- [ ] Updated documentation
- [ ] Added tests that prove my fix/feature works
- [ ] New and existing tests pass locally
- [ ] No new warnings introduced

## Related Issues
Closes #<issue_number>
```

### Review Process

1. Maintainers will review your PR
2. Address any requested changes
3. Once approved, your PR will be merged
4. Your contribution will be credited in release notes

---

## 🐛 Reporting Bugs

### Before Reporting

1. **Check existing issues** - It may already be reported
2. **Try latest version** - Bug might be fixed
3. **Verify it's reproducible** - Can you reproduce it consistently?

### Bug Report Template

```markdown
**Describe the bug**
Clear description of what the bug is.

**To Reproduce**
Steps to reproduce:
1. Call endpoint '...'
2. With parameters '...'
3. See error

**Expected behavior**
What you expected to happen.

**Actual behavior**
What actually happened.

**Environment:**
- OS: [e.g., Ubuntu 22.04]
- Java version: [e.g., 17.0.8]
- Spring Boot version: [e.g., 3.5.6]
- Deployment: [Docker/JAR/K8s]

**Logs**
```
Paste relevant logs here
```

**Additional context**
Any other relevant information.
```

---

## 💡 Suggesting Features

### Feature Request Template

```markdown
**Is your feature related to a problem?**
Clear description of the problem.

**Describe the solution you'd like**
What you want to happen.

**Describe alternatives you've considered**
Other solutions you've thought about.

**Additional context**
Mockups, examples, etc.

**Would you like to implement this feature?**
- [ ] Yes, I can work on this
- [ ] No, but I can help test
- [ ] No, just suggesting
```

---

## 📚 Documentation Guidelines

### Code Documentation

- **Public APIs:** Full Javadoc required
- **Private methods:** Optional but encouraged
- **Complex logic:** Explain the "why", not just "what"

### User Documentation

Located in `guides/`:
- **INSTALLATION.md** - Setup instructions
- **USAGE.md** - API usage examples
- **CONFIGURATION.md** - Configuration options
- **DEPLOYMENT.md** - Production deployment
- **TROUBLESHOOTING.md** - Common issues

### Format

- Use markdown
- Include code examples
- Add screenshots if helpful
- Keep language clear and concise

---

## 🧪 Testing Guidelines

### Writing Tests

```java
@SpringBootTest
class DrugServiceTest {

    @Mock
    private PuphaxSoapClient soapClient;

    @InjectMocks
    private DrugService drugService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchDrugs_validTerm_returnsResults() {
        // Arrange
        String searchTerm = "aspirin";
        String mockXml = "<drugs><drug><id>123</id><name>ASPIRIN</name></drug></drugs>";
        when(soapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXml));

        // Act
        DrugSearchResponse response = drugService.searchDrugs(searchTerm, null, null, 0, 20, "name", "ASC");

        // Assert
        assertNotNull(response);
        assertFalse(response.drugs().isEmpty());
        assertEquals("123", response.drugs().get(0).id());
    }
}
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=DrugServiceTest

# With coverage
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

---

## 🏆 Recognition

Contributors will be:
- Listed in release notes
- Credited in commit history
- Added to CONTRIBUTORS file (if created)
- Mentioned in project documentation

---

## 📞 Getting Help

**Need help contributing?**

- 💬 Open a [Discussion](https://github.com/Zsolaj123/PUPHAX-service/discussions)
- 📧 Contact maintainers via GitHub
- 📖 Read [Developer Guide](../docs/PUPHAX_DEVELOPMENT_GUIDE.md)

---

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for contributing to PUPHAX Service! 🎉**

---

**Last Updated:** 2025-10-23
