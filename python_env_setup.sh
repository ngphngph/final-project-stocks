#!/bin/bash

# 如果已有 bootcamp-env，先清除
if type deactivate &> /dev/null; then
    echo "🔄 Deactivating environment..."
    deactivate
fi

if [ -d "bootcamp-env" ]; then
    echo "🗑️  Removing old virtual environment (if any)..."
    rm -rf bootcamp-env
fi

# 建立新虛擬環境
echo "📦 Creating new virtual environment..."
python3 -m venv bootcamp-env

echo "✅ Activating virtual environment..."
source bootcamp-env/bin/activate

# 安裝依賴
pip install --upgrade pip
pip install -r requirements.txt

# 安裝 Jupyter kernel
python -m ipykernel install --user --name=bootcamp-env --display-name "Python (bootcamp-env)"

# 驗證
pip --version
python --version
jupyter --version

echo "✅ Environment Setup complete."