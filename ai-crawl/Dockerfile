FROM unclecode/crawl4ai:latest

ENV OPENBLAS_NUM_THREADS=1

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --progress-bar off -r requirements.txt

COPY . .

CMD ["python", "scheduler.py"]
