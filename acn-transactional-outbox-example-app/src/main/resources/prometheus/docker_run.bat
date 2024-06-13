@echo off

REM Check if prometheus container is running
set CONTAINER_RUNNING_ID=
for /f "tokens=*" %%i in ('docker ps -q --filter "name=prometheus"') do set CONTAINER_RUNNING_ID=%%i

if not "%CONTAINER_RUNNING_ID%"=="" (
    echo Prometheus container is already running.
    goto end
)

REM Check if prometheus container exists but is not running
set CONTAINER_EXISTS_ID=
for /f "tokens=*" %%i in ('docker ps -a -q --filter "name=prometheus"') do set CONTAINER_EXISTS_ID=%%i

if not "%CONTAINER_EXISTS_ID%"=="" (
    echo Starting existing Prometheus container...
    docker start prometheus
    goto end
)

REM Run a new Prometheus container
echo Running a new Prometheus container...
docker run -d ^
    --name=prometheus ^
    -p 9090:9090 ^
    -v "%cd%/prometheus.yml:/etc/prometheus/prometheus.yml" ^
    prom/prometheus

:end
echo Prometheus is running at http://localhost:9090
pause