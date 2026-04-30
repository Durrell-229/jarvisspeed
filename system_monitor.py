"""
Module de surveillance système pour JARVIS
Fournit des données réelles sur le CPU, mémoire, réseau, etc.
"""
import psutil
import time
from datetime import datetime

class SystemMonitor:
    def __init__(self):
        self.start_time = time.time()
        self.net_io_before = psutil.net_io_counters()
        self.time_before = time.time()

    def get_cpu_usage(self):
        """Récupère l'utilisation du CPU en pourcentage"""
        return psutil.cpu_percent(interval=0.1)

    def get_memory_usage(self):
        """Récupère l'utilisation de la mémoire"""
        mem = psutil.virtual_memory()
        return {
            'percent': mem.percent,
            'total': mem.total,
            'available': mem.available,
            'used': mem.used
        }

    def get_network_usage(self):
        """Récupère l'utilisation du réseau"""
        net_io = psutil.net_io_counters()
        current_time = time.time()
        time_delta = current_time - self.time_before

        upload_speed = (net_io.bytes_sent - self.net_io_before.bytes_sent) / max(time_delta, 0.001)
        download_speed = (net_io.bytes_recv - self.net_io_before.bytes_recv) / max(time_delta, 0.001)

        self.net_io_before = net_io
        self.time_before = current_time

        return {
            'upload_speed': upload_speed,
            'download_speed': download_speed,
            'bytes_sent': net_io.bytes_sent,
            'bytes_recv': net_io.bytes_recv
        }

    def get_disk_usage(self):
        """Récupère l'utilisation du disque"""
        disk = psutil.disk_usage('/')
        return {
            'percent': disk.percent,
            'total': disk.total,
            'free': disk.free,
            'used': disk.used
        }

    def get_system_info(self):
        """Récupère toutes les informations système"""
        return {
            'cpu': self.get_cpu_usage(),
            'memory': self.get_memory_usage(),
            'network': self.get_network_usage(),
            'disk': self.get_disk_usage(),
            'timestamp': datetime.now().isoformat(),
            'uptime': time.time() - self.start_time
        }

    def get_uptime_formatted(self):
        """Retourne le temps d'activité formaté"""
        uptime = time.time() - self.start_time
        hours = int(uptime // 3600)
        minutes = int((uptime % 3600) // 60)
        seconds = int(uptime % 60)
        return f"{hours:02d}:{minutes:02d}:{seconds:02d}"
