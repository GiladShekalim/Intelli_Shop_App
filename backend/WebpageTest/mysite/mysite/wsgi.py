"""
WSGI config for mysite project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/5.2/howto/deployment/wsgi/
"""

import os
import logging
from django.core.wsgi import get_wsgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'mysite.settings')

application = get_wsgi_application()

# Initialize database in production environment
logger = logging.getLogger(__name__)
try:
    from intellishop.utils.initialize_db import initialize_database
    logger.info("Initializing database on production startup...")
    initialize_database()
except Exception as e:
    logger.error(f"Error initializing database in production: {str(e)}")
