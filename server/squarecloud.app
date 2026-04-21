MAIN=main.py
MEMORY=512
VERSION=recommended
DISPLAY_NAME=NoveFlix API
DESCRIPTION=Servidor de streaming para o app NoveFlix
PORT=8080
START=uvicorn main:app --host 0.0.0.0 --port 8080 --workers 2
