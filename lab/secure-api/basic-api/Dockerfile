FROM python:3.13-slim
WORKDIR /app
COPY server.py /app/server.py
ENV ACRA_LAB_MODE=secure PORT=18082
EXPOSE 18082
CMD ["python","/app/server.py"]
