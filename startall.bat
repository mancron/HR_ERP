
echo RAG 서버 시작...
start "RAG Server" cmd /k "cd /d %~dp0rag && venv\Scripts\activate && python rag_server.py"
timeout /t 5 /nobreak > nul

