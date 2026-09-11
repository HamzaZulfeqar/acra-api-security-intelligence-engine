FROM python:3.13-slim
WORKDIR /app
COPY server.py /app/server.py
ENV ACRA_LAB_MODE=vulnerable PORT=18081
EXPOSE 18081
CMD ["python","/app/server.py"]
