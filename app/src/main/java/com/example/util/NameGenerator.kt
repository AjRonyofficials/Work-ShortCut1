package com.example.util

import kotlin.random.Random

data class CountryOption(
    val code: String,
    val name: String,
    val flag: String
)

enum class Gender {
    MALE, FEMALE, ANY
}

object NameGenerator {

    val commonCountries = listOf(
        CountryOption("BD", "Bangladesh", "🇧🇩"),
        CountryOption("US", "United States", "🇺🇸"),
        CountryOption("UK", "United Kingdom", "🇬🇧"),
        CountryOption("IN", "India", "🇮🇳"),
        CountryOption("CA", "Canada", "🇨🇦"),
        CountryOption("FR", "France", "🇫🇷"),
        CountryOption("DE", "Germany", "🇩🇪"),
        CountryOption("AU", "Australia", "🇦🇺"),
        CountryOption("JP", "Japan", "🇯🇵"),
        CountryOption("SA", "Saudi Arabia", "🇸🇦"),
        CountryOption("AE", "United Arab Emirates", "🇦🇪"),
        CountryOption("BR", "Brazil", "🇧🇷"),
        CountryOption("IT", "Italy", "🇮🇹"),
        CountryOption("TR", "Turkey", "🇹🇷"),
        CountryOption("PK", "Pakistan", "🇵🇰"),
        CountryOption("ES", "Spain", "🇪🇸")
    )

    val supportedCountries = commonCountries

    val allCountries = listOf(
        CountryOption("AF", "Afghanistan", "🇦🇫"),
        CountryOption("AL", "Albania", "🇦🇱"),
        CountryOption("DZ", "Algeria", "🇩🇿"),
        CountryOption("AR", "Argentina", "🇦🇷"),
        CountryOption("AM", "Armenia", "🇦🇲"),
        CountryOption("AU", "Australia", "🇦🇺"),
        CountryOption("AT", "Austria", "🇦🇹"),
        CountryOption("AZ", "Azerbaijan", "🇦🇿"),
        CountryOption("BH", "Bahrain", "🇧🇭"),
        CountryOption("BD", "Bangladesh", "🇧🇩"),
        CountryOption("BY", "Belarus", "🇧🇾"),
        CountryOption("BE", "Belgium", "🇧🇪"),
        CountryOption("BO", "Bolivia", "🇧🇴"),
        CountryOption("BA", "Bosnia", "🇧🇦"),
        CountryOption("BR", "Brazil", "🇧🇷"),
        CountryOption("BG", "Bulgaria", "🇧🇬"),
        CountryOption("KH", "Cambodia", "🇰🇭"),
        CountryOption("CA", "Canada", "🇨🇦"),
        CountryOption("CL", "Chile", "🇨🇱"),
        CountryOption("CN", "China", "🇨🇳"),
        CountryOption("CO", "Colombia", "🇨🇴"),
        CountryOption("CR", "Costa Rica", "🇨🇷"),
        CountryOption("HR", "Croatia", "🇭🇷"),
        CountryOption("CY", "Cyprus", "🇨🇾"),
        CountryOption("CZ", "Czech Republic", "🇨🇿"),
        CountryOption("DK", "Denmark", "🇩🇰"),
        CountryOption("EG", "Egypt", "🇪🇬"),
        CountryOption("EE", "Estonia", "🇪🇪"),
        CountryOption("FI", "Finland", "🇫🇮"),
        CountryOption("FR", "France", "🇫🇷"),
        CountryOption("GE", "Georgia", "🇬🇪"),
        CountryOption("DE", "Germany", "🇩🇪"),
        CountryOption("GH", "Ghana", "🇬🇭"),
        CountryOption("GR", "Greece", "🇬🇷"),
        CountryOption("HK", "Hong Kong", "🇭🇰"),
        CountryOption("HU", "Hungary", "🇭🇺"),
        CountryOption("IS", "Iceland", "🇮🇸"),
        CountryOption("IN", "India", "🇮🇳"),
        CountryOption("ID", "Indonesia", "🇮🇩"),
        CountryOption("IR", "Iran", "🇮🇷"),
        CountryOption("IQ", "Iraq", "🇮🇶"),
        CountryOption("IE", "Ireland", "🇮🇪"),
        CountryOption("IL", "Israel", "🇮🇱"),
        CountryOption("IT", "Italy", "🇮🇹"),
        CountryOption("JM", "Jamaica", "🇯🇲"),
        CountryOption("JP", "Japan", "🇯🇵"),
        CountryOption("JO", "Jordan", "🇯🇴"),
        CountryOption("KZ", "Kazakhstan", "🇰🇿"),
        CountryOption("KE", "Kenya", "🇰🇪"),
        CountryOption("KW", "Kuwait", "🇰🇼"),
        CountryOption("LB", "Lebanon", "🇱🇧"),
        CountryOption("MY", "Malaysia", "🇲🇾"),
        CountryOption("MV", "Maldives", "🇲🇻"),
        CountryOption("MX", "Mexico", "🇲🇽"),
        CountryOption("MA", "Morocco", "🇲🇦"),
        CountryOption("NP", "Nepal", "🇳🇵"),
        CountryOption("NL", "Netherlands", "🇳🇱"),
        CountryOption("NZ", "New Zealand", "🇳🇿"),
        CountryOption("NG", "Nigeria", "🇳🇬"),
        CountryOption("NO", "Norway", "🇳🇴"),
        CountryOption("OM", "Oman", "🇴🇲"),
        CountryOption("PK", "Pakistan", "🇵🇰"),
        CountryOption("PA", "Panama", "🇵🇦"),
        CountryOption("PE", "Peru", "🇵🇪"),
        CountryOption("PH", "Philippines", "🇵🇭"),
        CountryOption("PL", "Poland", "🇵🇱"),
        CountryOption("PT", "Portugal", "🇵🇹"),
        CountryOption("QA", "Qatar", "🇶🇦"),
        CountryOption("RO", "Romania", "🇷🇴"),
        CountryOption("RU", "Russia", "🇷🇺"),
        CountryOption("SA", "Saudi Arabia", "🇸🇦"),
        CountryOption("RS", "Serbia", "🇷🇸"),
        CountryOption("SG", "Singapore", "🇸🇬"),
        CountryOption("ZA", "South Africa", "🇿🇦"),
        CountryOption("KR", "South Korea", "🇰🇷"),
        CountryOption("ES", "Spain", "🇪🇸"),
        CountryOption("LK", "Sri Lanka", "🇱🇰"),
        CountryOption("SE", "Sweden", "🇸🇪"),
        CountryOption("CH", "Switzerland", "🇨🇭"),
        CountryOption("TW", "Taiwan", "🇹🇼"),
        CountryOption("TH", "Thailand", "🇹🇭"),
        CountryOption("TR", "Turkey", "🇹🇷"),
        CountryOption("UA", "Ukraine", "🇺🇦"),
        CountryOption("AE", "United Arab Emirates", "🇦🇪"),
        CountryOption("UK", "United Kingdom", "🇬🇧"),
        CountryOption("US", "United States", "🇺🇸"),
        CountryOption("UZ", "Uzbekistan", "🇺🇿"),
        CountryOption("VN", "Vietnam", "🇻🇳"),
        CountryOption("YE", "Yemen", "🇾🇪"),
        CountryOption("ZW", "Zimbabwe", "🇿🇼")
    )

    private val namesByCountry: Map<String, CountryNames> = mapOf(
        "BD" to CountryNames(
            maleFirst = listOf(
                "Tanvir", "Rafiqul", "Sabbir", "Arif", "Mahmudul", "Hasan", "Shakil", "Rashed",
                "Tareq", "Kamrul", "Nazmul", "Ashik", "Mehedi", "Faisal", "Imran", "Farhan",
                "Zubair", "Rayhan", "Tamim", "Mustafiz", "Anik", "Nayeem", "Saiful", "Sharif"
            ),
            femaleFirst = listOf(
                "Nusrat", "Farhana", "Sumaiya", "Tanjina", "Sharmin", "Sultana", "Sadia", "Rimi",
                "Jannatul", "Afsana", "Tasnim", "Mousumi", "Nabila", "Sabrina", "Fariha", "Anika",
                "Samia", "Priyanka", "Rumana", "Tania", "Mehnaz", "Nazia", "Mithila", "Lamia"
            ),
            lastNames = listOf(
                "Ahmed", "Islam", "Hasan", "Rahman", "Hossain", "Chowdhury", "Khan", "Ali",
                "Akter", "Khatun", "Uddin", "Bhuiyan", "Sikder", "Miah", "Sarkar", "Kabir"
            )
        ),
        "US" to CountryNames(
            maleFirst = listOf(
                "James", "Michael", "Ethan", "Alexander", "Daniel", "Matthew", "Lucas", "Noah",
                "Oliver", "William", "Benjamin", "Henry", "Jackson", "Mason", "Jack", "Samuel"
            ),
            femaleFirst = listOf(
                "Emma", "Olivia", "Ava", "Sophia", "Isabella", "Mia", "Charlotte", "Amelia",
                "Harper", "Evelyn", "Abigail", "Emily", "Elizabeth", "Chloe", "Grace", "Zoey"
            ),
            lastNames = listOf(
                "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson",
                "Anderson", "Taylor", "Thomas", "Moore", "Jackson", "Martin", "Lee", "White"
            )
        ),
        "UK" to CountryNames(
            maleFirst = listOf(
                "Oliver", "George", "Arthur", "Noah", "Muhammad", "Leo", "Oscar", "Harry",
                "Archie", "Jack", "Henry", "Charlie", "Freddie", "Theodore", "Thomas", "Finley"
            ),
            femaleFirst = listOf(
                "Olivia", "Amelia", "Isla", "Ava", "Mia", "Ivy", "Lily", "Isabella",
                "Rosie", "Sophia", "Grace", "Freya", "Willow", "Florence", "Emily", "Ella"
            ),
            lastNames = listOf(
                "Smith", "Jones", "Taylor", "Brown", "Williams", "Wilson", "Johnson", "Davies",
                "Robinson", "Wright", "Thompson", "Evans", "Walker", "White", "Roberts", "Green"
            )
        ),
        "IN" to CountryNames(
            maleFirst = listOf(
                "Aarav", "Vihaan", "Aditya", "Arjun", "Reyansh", "Sai", "Aayush", "Rohan",
                "Ishaan", "Dhruv", "Kabir", "Aryan", "Aniket", "Dev", "Vikram", "Rahul"
            ),
            femaleFirst = listOf(
                "Aadhya", "Diya", "Saanvi", "Ananya", "Pari", "Isha", "Navya", "Riya",
                "Myra", "Kavya", "Avani", "Tanvi", "Shreya", "Anushka", "Pooja", "Deepika"
            ),
            lastNames = listOf(
                "Sharma", "Verma", "Gupta", "Malhotra", "Bhatia", "Saxena", "Mehta", "Patel",
                "Chopra", "Reddy", "Nair", "Iyer", "Mukherjee", "Banerjee", "Singh", "Das"
            )
        ),
        "CA" to CountryNames(
            maleFirst = listOf("Liam", "Noah", "William", "Lucas", "Leo", "Benjamin", "Oliver", "Jack", "Nathan", "Felix"),
            femaleFirst = listOf("Olivia", "Emma", "Charlotte", "Amelia", "Sophia", "Chloe", "Mia", "Alice", "Florence", "Lea"),
            lastNames = listOf("Tremblay", "Roy", "Gagnon", "Cote", "Bouchard", "Smith", "Brown", "Campbell", "MacDonald", "Stewart")
        ),
        "DE" to CountryNames(
            maleFirst = listOf("Lukas", "Leon", "Finn", "Paul", "Jonas", "Felix", "Maximilian", "Tim", "Niklas", "Jan"),
            femaleFirst = listOf("Emma", "Mia", "Hannah", "Emilia", "Sofia", "Lina", "Marie", "Mila", "Ella", "Clara"),
            lastNames = listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann")
        ),
        "AU" to CountryNames(
            maleFirst = listOf("Oliver", "Noah", "William", "Jack", "Leo", "Henry", "Charlie", "Thomas", "Lucas", "Hudson"),
            femaleFirst = listOf("Charlotte", "Olivia", "Amelia", "Isla", "Mia", "Ava", "Grace", "Willow", "Harper", "Chloe"),
            lastNames = listOf("Smith", "Jones", "Williams", "Brown", "Wilson", "Taylor", "Johnson", "White", "Martin", "Anderson")
        ),
        "FR" to CountryNames(
            maleFirst = listOf("Gabriel", "Leo", "Raphael", "Arthur", "Louis", "Lucas", "Adam", "Jules", "Hugo", "Mael"),
            femaleFirst = listOf("Jade", "Louise", "Emma", "Alice", "Ambre", "Lina", "Rose", "Chloe", "Mia", "Lea"),
            lastNames = listOf("Martin", "Bernard", "Thomas", "Petit", "Robert", "Richard", "Durand", "Dubois", "Moreau", "Laurent")
        ),
        "JP" to CountryNames(
            maleFirst = listOf("Ren", "Haruto", "Souta", "Yuto", "Riku", "Kaito", "Hinata", "Minato", "Asahi", "Takumi"),
            femaleFirst = listOf("Himari", "Hina", "Yua", "Sakura", "Ichika", "Akari", "Sara", "Yui", "Mei", "Rio"),
            lastNames = listOf("Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura", "Kobayashi", "Kato")
        ),
        "SA" to CountryNames(
            maleFirst = listOf("Mohammed", "Abdullah", "Abdulaziz", "Fahad", "Saud", "Omar", "Khaled", "Ali", "Sultan", "Nasser"),
            femaleFirst = listOf("Fatima", "Sarah", "Reem", "Noura", "Lama", "Haya", "Maha", "Hanan", "Leen", "Danah"),
            lastNames = listOf("Al-Ghamdi", "Al-Zahrani", "Al-Otaibi", "Al-Shehri", "Al-Harbi", "Al-Dossari", "Al-Qahtani", "Al-Mutairi")
        ),
        "AE" to CountryNames(
            maleFirst = listOf("Zayed", "Rashid", "Hamdan", "Saeed", "Mansoor", "Majid", "Khalifa", "Sultan", "Hazza", "Maktoum"),
            femaleFirst = listOf("Mariam", "Fatima", "Shamsa", "Meera", "Latifa", "Salama", "Mouza", "Maitha", "Afra", "Hind"),
            lastNames = listOf("Al-Falasi", "Al-Maktoum", "Al-Nuaimi", "Al-Suwaidi", "Al-Zaabi", "Al-Mazrouei", "Al-Ketbi", "Al-Ameri")
        ),
        "BR" to CountryNames(
            maleFirst = listOf("Miguel", "Arthur", "Heitor", "Bernardo", "Davi", "Gabriel", "Pedro", "Lorenzo", "Lucas", "Matheus"),
            femaleFirst = listOf("Helena", "Alice", "Laura", "Manuela", "Sophia", "Isabella", "Luiza", "Valentina", "Giovanna", "Maria"),
            lastNames = listOf("Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira", "Lima", "Gomes")
        ),
        "IT" to CountryNames(
            maleFirst = listOf("Leonardo", "Francesco", "Alessandro", "Lorenzo", "Mattia", "Andrea", "Gabriele", "Riccardo", "Tommaso", "Edoardo"),
            femaleFirst = listOf("Sofia", "Giulia", "Aurora", "Ginevra", "Alice", "Beatrice", "Emma", "Giorgia", "Vittoria", "Matilde"),
            lastNames = listOf("Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo", "Ricci", "Marino", "Greco")
        ),
        "TR" to CountryNames(
            maleFirst = listOf("Yusuf", "Eymen", "Ömer", "Miraç", "Kerem", "Alparslan", "Mustafa", "Ali", "Ahmet", "Emir"),
            femaleFirst = listOf("Zeynep", "Elif", "Defne", "Asel", "Azra", "Eylül", "Nehir", "Ecrin", "Meryem", "Zehra"),
            lastNames = listOf("Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir")
        ),
        "PK" to CountryNames(
            maleFirst = listOf("Muhammad", "Ahmed", "Hamza", "Bilal", "Ali", "Usman", "Hassan", "Zain", "Farhan", "Shahid"),
            femaleFirst = listOf("Ayesha", "Fatima", "Zainab", "Maryam", "Sana", "Hira", "Anum", "Laiba", "Khadija", "Nimra"),
            lastNames = listOf("Khan", "Malik", "Chaudhry", "Bhatti", "Butt", "Qureshi", "Siddiqui", "Abbasi", "Sheikh", "Raza")
        ),
        "ES" to CountryNames(
            maleFirst = listOf("Hugo", "Mateo", "Martin", "Lucas", "Leo", "Daniel", "Alejandro", "Manuel", "Pablo", "Alvaro"),
            femaleFirst = listOf("Lucia", "Sofia", "Martina", "Maria", "Julia", "Paula", "Valeria", "Emma", "Alba", "Sara"),
            lastNames = listOf("Garcia", "Rodriguez", "Gonzalez", "Fernandez", "Lopez", "Martinez", "Sanchez", "Perez", "Gomez", "Martin")
        )
    )

    fun generateName(countryCode: String, gender: Gender = Gender.ANY): String {
        val code = countryCode.uppercase()
        val data = namesByCountry[code] ?: when (code) {
            "SA", "AE", "EG", "QA", "KW", "BH", "OM", "JO", "LB", "IQ", "DZ", "MA", "YE" -> namesByCountry["SA"]!!
            "ES", "MX", "AR", "CO", "PE", "CL", "CR", "PA", "UY", "BO" -> namesByCountry["ES"]!!
            "PT", "BR" -> namesByCountry["BR"]!!
            "IN", "NP", "LK" -> namesByCountry["IN"]!!
            "PK", "AF" -> namesByCountry["PK"]!!
            "BD" -> namesByCountry["BD"]!!
            "FR", "BE", "LU" -> namesByCountry["FR"]!!
            "DE", "AT", "CH", "NL" -> namesByCountry["DE"]!!
            "IT" -> namesByCountry["IT"]!!
            "TR", "AZ" -> namesByCountry["TR"]!!
            "JP" -> namesByCountry["JP"]!!
            "UK", "IE" -> namesByCountry["UK"]!!
            "AU", "NZ" -> namesByCountry["AU"]!!
            "CA" -> namesByCountry["CA"]!!
            else -> namesByCountry["US"]!!
        }
        val isMale = when (gender) {
            Gender.MALE -> true
            Gender.FEMALE -> false
            Gender.ANY -> Random.nextBoolean()
        }
        val pool = if (isMale) data.maleFirst else data.femaleFirst
        val firstName = pool[Random.nextInt(pool.size)]
        val lastName = data.lastNames[Random.nextInt(data.lastNames.size)]
        return "$firstName $lastName"
    }

    fun getCountryOption(countryCode: String): CountryOption {
        return allCountries.firstOrNull { it.code.equals(countryCode, ignoreCase = true) }
            ?: commonCountries.firstOrNull { it.code.equals(countryCode, ignoreCase = true) }
            ?: CountryOption("BD", "Bangladesh", "🇧🇩")
    }

    private data class CountryNames(
        val maleFirst: List<String>,
        val femaleFirst: List<String>,
        val lastNames: List<String>
    )
}
