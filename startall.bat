@echo off
echo [1/3] Ollama 시작...
start "Ollama" cmd /k "ollama serve"
timeout /t 3 /nobreak > nul

echo [2/3] RAG 서버 시작...
start "RAG Server" cmd /k "cd /d %~dp0rag && venv\Scripts\activate && python rag_server.py"
timeout /t 5 /nobreak > nul

echo [3/3] Tomcat 시작...
start "Tomcat" cmd /k "C:\ERP\apache-tomcat-10.1.36\bin\startup.bat"

echo 모든 서버 기동 완료