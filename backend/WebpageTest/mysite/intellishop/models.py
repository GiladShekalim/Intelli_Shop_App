# Database models defining the data structure for IntelliShop

from django.db import models

class User(models.Model):
    username = models.CharField(max_length=100, unique=True)
    password = models.CharField(max_length=100)  # In production, use proper password hashing
    email = models.EmailField(unique=True)
    status = models.JSONField(default=list)  # Changed to JSONField to store multiple statuses
    age = models.IntegerField()
    location = models.CharField(max_length=100)
    hobbies = models.JSONField(default=list)
    created_at = models.DateTimeField(auto_now_add=True)
    
    def __str__(self):
        return self.username
