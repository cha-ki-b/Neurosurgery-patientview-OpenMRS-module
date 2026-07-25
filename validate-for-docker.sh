#!/bin/bash
# =============================================================================
# QUICK VALIDATION SCRIPT FOR DOCKER DEPLOYMENT
# File: validate-for-docker.sh
# =============================================================================

echo "=========================================="
echo "Quick OpenMRS Module Validation"
echo "=========================================="

# Function to check if command succeeded
check_result() {
    if [ $? -eq 0 ]; then
        echo "✅ $1 - PASSED"
    else
        echo "❌ $1 - FAILED"
        exit 1
    fi
}

# 1. Quick compilation test
echo "1. Testing compilation..."
mvn clean compile -q
check_result "Compilation"

# 2. Run only the connection tests (fast)
echo "2. Running DAO connection tests..."
mvn test -Dtest=PatientviewServiceImplConnectionTest -q
check_result "DAO Connection Test"

# 3. Test Spring context loading
echo "3. Testing Spring context..."
mvn test -Dtest=PatientviewServiceSpringTest -q
check_result "Spring Context Test"

# 4. Quick deployment validation
echo "4. Running deployment validation..."
mvn test -Dtest=QuickDeploymentTest -q
check_result "Deployment Validation"

# 5. Package the module
echo "5. Creating module package..."
mvn package -DskipTests -q
check_result "Module Packaging"

# Check if OMOD file was created
if [ -f "omod/target/*.omod" ]; then
    echo "✅ OMOD file created successfully"
else
    echo "❌ OMOD file not found"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ ALL VALIDATION TESTS PASSED!"
echo "Your module is ready for Docker deployment"
echo "=========================================="