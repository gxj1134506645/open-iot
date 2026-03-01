#!/bin/bash

# ========================================
# Open-IoT 构建脚本
# 用法: ./scripts/build.sh [选项]
#   -b, --backend     仅构建后端
#   -f, --frontend    仅构建前端
#   -a, --all         构建全部（默认）
#   -s, --skip-test   跳过测试
#   -c, --clean       清理后构建
#   -h, --help        显示帮助
# ========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认参数
BUILD_BACKEND=true
BUILD_FRONTEND=true
SKIP_TEST=false
CLEAN_BUILD=false

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--backend)
            BUILD_BACKEND=true
            BUILD_FRONTEND=false
            shift
            ;;
        -f|--frontend)
            BUILD_BACKEND=false
            BUILD_FRONTEND=true
            shift
            ;;
        -a|--all)
            BUILD_BACKEND=true
            BUILD_FRONTEND=true
            shift
            ;;
        -s|--skip-test)
            SKIP_TEST=true
            shift
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -h|--help)
            echo "用法: ./scripts/build.sh [选项]"
            echo ""
            echo "选项:"
            echo "  -b, --backend     仅构建后端"
            echo "  -f, --frontend    仅构建前端"
            echo "  -a, --all         构建全部（默认）"
            echo "  -s, --skip-test   跳过测试"
            echo "  -c, --clean       清理后构建"
            echo "  -h, --help        显示帮助"
            exit 0
            ;;
        *)
            echo -e "${RED}未知选项: $1${NC}"
            exit 1
            ;;
    esac
done

# 获取项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Open-IoT 构建脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 构建后端
build_backend() {
    echo -e "${YELLOW}[1/2] 构建后端...${NC}"
    cd "$PROJECT_ROOT/backend"

    MVN_ARGS=""
    if [ "$SKIP_TEST" = true ]; then
        MVN_ARGS="-DskipTests"
        echo -e "  ${YELLOW}跳过测试${NC}"
    fi

    if [ "$CLEAN_BUILD" = true ]; then
        MVN_ARGS="$MVN_ARGS clean"
        echo -e "  ${YELLOW}清理构建${NC}"
    fi

    mvn $MVN_ARGS package -DskipTests

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 后端构建成功${NC}"
    else
        echo -e "${RED}✗ 后端构建失败${NC}"
        exit 1
    fi
}

# 构建前端
build_frontend() {
    echo -e "${YELLOW}[2/2] 构建前端...${NC}"
    cd "$PROJECT_ROOT/frontend"

    # 检查 node_modules 是否存在
    if [ ! -d "node_modules" ]; then
        echo -e "  ${YELLOW}安装依赖...${NC}"
        npm install
    fi

    npm run build

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 前端构建成功${NC}"
        echo -e "  构建产物: ${BLUE}$PROJECT_ROOT/frontend/dist${NC}"
    else
        echo -e "${RED}✗ 前端构建失败${NC}"
        exit 1
    fi
}

# 执行构建
START_TIME=$(date +%s)

if [ "$BUILD_BACKEND" = true ]; then
    build_backend
    echo ""
fi

if [ "$BUILD_FRONTEND" = true ]; then
    build_frontend
    echo ""
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  构建完成！耗时: ${DURATION}s${NC}"
echo -e "${GREEN}========================================${NC}"
