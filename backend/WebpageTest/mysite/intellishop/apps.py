# Application configuration settings for the IntelliShop app

from django.apps import AppConfig
import logging

logger = logging.getLogger(__name__)

class IntellishopConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'intellishop'
    
    def ready(self):
        """
        Initialize database when Django starts.
        This method is called once the application is ready.
        """
        try:
            # Import here to avoid circular imports
            from intellishop.utils.initialize_db import initialize_database
            
            # Only initialize the database when running the web server
            # This prevents initialization during management commands
            import sys
            if 'runserver' in sys.argv or 'gunicorn' in sys.argv[0]:
                logger.info("Initializing database on application startup...")
                initialize_database()
                
        except Exception as e:
            logger.error(f"Error during application startup: {str(e)}")
