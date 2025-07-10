#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "🚀 Running json_generator.py..."
if python3 json_generator.py; then
    echo -e "${GREEN}✅ json_generator.py ran successfully${NC}"
else
    echo -e "${RED}❌ Failed to run json_generator.py${NC}"
    exit 1
fi

echo -e "\n📊 Running ExcelGenerator.py..."
if python3 ExcelGenerator.py; then
    echo -e "${GREEN}🎉 ExcelGenerator.py ran successfully${NC}"
else
    echo -e "${RED}❌ Failed to run ExcelGenerator.py${NC}"
    exit 1
fi

echo -e "\n🔄 Switching to parent directory and running scripts/render_routes.py..."
cd ..
if python3 scripts/render_routes.py; then
    echo -e "${GREEN}✅ render_routes.py ran successfully${NC}"
else
    echo -e "${RED}❌ Failed to run render_routes.py${NC}"
    exit 1
fi
