#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📁 Changing directory to python_component...${NC}"
cd python_component || {
    echo -e "${RED}❌ Failed to change directory to python_component${NC}"
    exit 1
}

echo -e "${BLUE}🚀 Running generate_report.sh...${NC}"
if bash generate_report.sh; then
    echo -e "${GREEN}✅ generate_report.sh completed successfully${NC}"
else
    echo -e "${RED}❌ Failed to run generate_report.sh${NC}"
    exit 1
fi

cd ..

echo -e "${BLUE}🧠 Checking available Python command...${NC}"
if command -v python &> /dev/null; then
    PYTHON_CMD="python"
elif command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
else
    echo -e "${RED}❌ Neither python nor python3 found in PATH${NC}"
    exit 1
fi

echo -e "${BLUE}🧩 Launching UI: $PYTHON_CMD ui-app/app.py${NC}"
$PYTHON_CMD ui-app/app.py
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to run UI script${NC}"
    exit 1
fi

echo -e "${GREEN}✅ UI launched successfully${NC}"
