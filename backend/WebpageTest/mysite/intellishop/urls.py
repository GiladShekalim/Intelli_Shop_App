# URL patterns that map URLs to view functions within the IntelliShop app
from django.urls import path
from . import views

urlpatterns = [
    path('', views.index, name='index'),  # Root route to index
    path('home/', views.index_home, name='index_home'),
    path('login/', views.login_view, name='login'),
    path('google_login/', views.google_login, name='google_login'),
    path('register/', views.register, name='register'),
    # index_home / dashboard / mfa_verification kept: they are redirect() targets.
    path('mfa_verification/', views.mfa_verification, name='mfa_verification'),
    path('dashboard/', views.dashboard, name='dashboard'),
    path('profile/', views.profile_view, name='profile'),
    path('logout/', views.logout_view, name='logout'),
    path('favorites/', views.favorites_view, name='favorites'),
    path('show_all_discounts/', views.show_all_discounts, name='show_all_discounts'),
    path('filtered_discounts/', views.filtered_discounts, name='filtered_discounts'),
    path('search_discounts/', views.search_discounts_by_text, name='search_discounts_by_text'),
    path('ai_filter_helper/', views.ai_filter_helper, name='ai_filter_helper'),
    path('add_favorite/', views.add_favorite_view, name='add_favorite'),
    path('remove_favorite/', views.remove_favorite_view, name='remove_favorite'),
    path('add_history/', views.add_history_view, name='add_history'),
    path('history/', views.history_view, name='history'),
    path('check_username/', views.check_username_view, name='check_username'),
    path('share_coupon/', views.share_coupon_view, name='share_coupon'),
    path('received_shares/', views.received_shares_view, name='received_shares'),
    path('remove_share/', views.remove_share_view, name='remove_share'),
    path('add_redeemed/', views.add_redeemed_view, name='add_redeemed'),
    path('redeemed/', views.redeemed_view, name='redeemed'),
    path('check_favorite/<str:discount_id>/', views.check_favorite_view, name='check_favorite'),
    path('api/club_names/', views.get_club_names, name='get_club_names'),
]

