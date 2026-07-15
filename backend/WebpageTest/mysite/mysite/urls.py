from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from intellishop import views

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', include('intellishop.urls')),  # Root URL will go to intellishop app
    path('coupon/<str:coupon_code>/', views.coupon_detail, name='coupon_detail'),
]

# Add this to serve static files in development
if settings.DEBUG:
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT) 