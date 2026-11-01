FROM mcr.microsoft.com/playwright/python:v1.48.0-jammy
ENV PYTHONUNBUFFERED=1
WORKDIR /app
COPY requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8001
CMD ["python", "start.py"]
