package org.totschnig.myexpenses.viewmodel.data

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.livefront.sealedenum.GenSealedEnum
import org.totschnig.myexpenses.R

sealed class IconCategory(
    @StringRes val label: Int,
    @ArrayRes val fontAweSomeIcons: Int,
    @ArrayRes val extraIcons: Int? = null
) {
    data object Accessibility: IconCategory(R.string.category_accessibility_label, R.array.category_accessibility_icons)
    data object Alert: IconCategory(R.string.category_alert_label, R.array.category_alert_icons)
    data object Alphabet: IconCategory(R.string.category_alphabet_label, R.array.category_alphabet_icons)
    data object Animals: IconCategory(R.string.category_animals_label, R.array.category_animals_icons)
    data object Arrows: IconCategory(R.string.category_arrows_label, R.array.category_arrows_icons)
    data object Astronomy: IconCategory(R.string.category_astronomy_label, R.array.category_astronomy_icons)
    data object Automotive: IconCategory(R.string.category_automotive_label, R.array.category_automotive_icons)
    data object Buildings: IconCategory(R.string.category_buildings_label, R.array.category_buildings_icons, R.array.extra_buildings_icons)
    data object Business: IconCategory(R.string.category_business_label, R.array.category_business_icons, R.array.extra_business_icons)
    data object Camping: IconCategory(R.string.category_camping_label, R.array.category_camping_icons)
    data object Charity: IconCategory(R.string.category_charity_label, R.array.category_charity_icons)
    data object ChartsDiagrams: IconCategory(R.string.category_charts_diagrams_label, R.array.category_charts_diagrams_icons)
    data object Childhood: IconCategory(R.string.category_childhood_label, R.array.category_childhood_icons, R.array.extra_childhood_icons)
    data object ClothingFashion: IconCategory(R.string.category_clothing_fashion_label, R.array.category_clothing_fashion_icons, R.array.extra_clothing_fashion_icons)
    data object Coding: IconCategory(R.string.category_coding_label, R.array.category_coding_icons)
    data object Communication: IconCategory(R.string.category_communication_label, R.array.category_communication_icons)
    data object Connectivity: IconCategory(R.string.category_connectivity_label, R.array.category_connectivity_icons)
    data object Construction: IconCategory(R.string.category_construction_label, R.array.category_construction_icons)
    data object Design: IconCategory(R.string.category_design_label, R.array.category_design_icons)
    data object DevicesHardware: IconCategory(R.string.category_devices_hardware_label, R.array.category_devices_hardware_icons)
    data object Disaster: IconCategory(R.string.category_disaster_label, R.array.category_disaster_icons)
    data object Editing: IconCategory(R.string.category_editing_label, R.array.category_editing_icons)
    data object Education: IconCategory(R.string.category_education_label, R.array.category_education_icons)
    data object Emoji: IconCategory(R.string.category_emoji_label, R.array.category_emoji_icons)
    data object Energy: IconCategory(R.string.category_energy_label, R.array.category_energy_icons)
    data object Files: IconCategory(R.string.category_files_label, R.array.category_files_icons)
    data object FilmVideo: IconCategory(R.string.category_film_video_label, R.array.category_film_video_icons)
    data object FoodBeverage: IconCategory(R.string.category_food_beverage_label, R.array.category_food_beverage_icons)
    data object FruitsVegetables: IconCategory(R.string.category_fruits_vegetables_label, R.array.category_fruits_vegetables_icons)
    data object Gaming: IconCategory(R.string.category_gaming_label, R.array.category_gaming_icons, R.array.extra_gaming_icons)
    data object Gender: IconCategory(R.string.category_gender_label, R.array.category_gender_icons)
    data object Halloween: IconCategory(R.string.category_halloween_label, R.array.category_halloween_icons)
    data object Hands: IconCategory(R.string.category_hands_label, R.array.category_hands_icons)
    data object Holidays: IconCategory(R.string.category_holidays_label, R.array.category_holidays_icons)
    data object Household: IconCategory(R.string.category_household_label, R.array.category_household_icons, R.array.extra_household_icons)
    data object Humanitarian: IconCategory(R.string.category_humanitarian_label, R.array.category_humanitarian_icons)
    data object Logistics: IconCategory(R.string.category_logistics_label, R.array.category_logistics_icons)
    data object Maps: IconCategory(R.string.category_maps_label, R.array.category_maps_icons)
    data object Maritime: IconCategory(R.string.category_maritime_label, R.array.category_maritime_icons)
    data object Marketing: IconCategory(R.string.category_marketing_label, R.array.category_marketing_icons)
    data object Mathematics: IconCategory(R.string.category_mathematics_label, R.array.category_mathematics_icons)
    data object MediaPlayback: IconCategory(R.string.category_media_playback_label, R.array.category_media_playback_icons)
    data object MedicalHealth: IconCategory(R.string.category_medical_health_label, R.array.category_medical_health_icons)
    data object Money: IconCategory(R.string.category_money_label, R.array.category_money_icons, R.array.extra_money_icons)
    data object Moving: IconCategory(R.string.category_moving_label, R.array.category_moving_icons)
    data object MusicAudio: IconCategory(R.string.category_music_audio_label, R.array.category_music_audio_icons)
    data object Nature: IconCategory(R.string.category_nature_label, R.array.category_nature_icons, R.array.extra_nature_icons)
    data object Numbers: IconCategory(R.string.category_numbers_label, R.array.category_numbers_icons)
    data object PhotosImages: IconCategory(R.string.category_photos_images_label, R.array.category_photos_images_icons)
    data object Political: IconCategory(R.string.category_political_label, R.array.category_political_icons, R.array.extra_political_icons)
    data object PunctuationSymbols: IconCategory(R.string.category_punctuation_symbols_label, R.array.category_punctuation_symbols_icons)
    data object Religion: IconCategory(R.string.category_religion_label, R.array.category_religion_icons)
    data object Science: IconCategory(R.string.category_science_label, R.array.category_science_icons)
    data object ScienceFiction: IconCategory(R.string.category_science_fiction_label, R.array.category_science_fiction_icons)
    data object Security: IconCategory(R.string.category_security_label, R.array.category_security_icons, R.array.extra_security_icons)
    data object Shapes: IconCategory(R.string.category_shapes_label, R.array.category_shapes_icons)
    data object Shopping: IconCategory(R.string.category_shopping_label, R.array.category_shopping_icons, R.array.extra_shopping_icons)
    data object Social: IconCategory(R.string.category_social_label, R.array.category_social_icons, R.array.extra_social_icons)
    data object Spinners: IconCategory(R.string.category_spinners_label, R.array.category_spinners_icons)
    data object SportsFitness: IconCategory(R.string.category_sports_fitness_label, R.array.category_sports_fitness_icons)
    data object TextFormatting: IconCategory(R.string.category_text_formatting_label, R.array.category_text_formatting_icons)
    data object Time: IconCategory(R.string.category_time_label, R.array.category_time_icons)
    data object Toggle: IconCategory(R.string.category_toggle_label, R.array.category_toggle_icons)
    data object Transportation: IconCategory(R.string.category_transportation_label, R.array.category_transportation_icons)
    data object TravelHotel: IconCategory(R.string.category_travel_hotel_label, R.array.category_travel_hotel_icons, R.array.extra_travel_hotel_icons)
    data object UsersPeople: IconCategory(R.string.category_users_people_label, R.array.category_users_people_icons, R.array.extra_users_people_icons)
    data object Weather: IconCategory(R.string.category_weather_label, R.array.category_weather_icons)
    data object Writing: IconCategory(R.string.category_writing_label, R.array.category_writing_icons)


    @GenSealedEnum
    companion object
}
