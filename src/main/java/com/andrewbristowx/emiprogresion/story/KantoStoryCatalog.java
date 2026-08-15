package com.andrewbristowx.emiprogresion.story;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Complete, coordinate-free Kanto campaign catalogue. Administrators place these
 * authored slots in the map; the catalogue owns identity, order, teams and text.
 */
public final class KantoStoryCatalog {
    private static final Map<String, NpcDefinition> NPCS = new LinkedHashMap<>();
    private static final Map<String, List<TrainerDefinition>> ROUTES = new LinkedHashMap<>();
    private static final Map<Integer, GymDefinition> GYMS = new LinkedHashMap<>();
    private static final Map<String, BossDefinition> BOSSES = new LinkedHashMap<>();
    private static final List<LeagueDefinition> LEAGUE = new ArrayList<>();

    static {
        registerNpcs();
        registerRoutes();
        registerGyms();
        registerBosses();
        registerLeague();
    }

    private KantoStoryCatalog() {}

    private static void registerNpcs() {
        npc("oak", "pueblo_paleta", "Profesor Oak", "professor_oak_00c8",
                "Oak te espera para iniciar oficialmente tu aventura por Kanto.",
                "Siempre puedes preguntarme cuál es tu siguiente objetivo.", "oak", false);
        npc("delia", "pueblo_paleta", "Delia", "pallet_delia",
                "Una aventura larga empieza cuidando bien de tu equipo. Vuelve cuando necesites ánimo.",
                "No olvides visitar los Centros Pokémon de cada ciudad.", "", false);
        npc("vecino_paleta", "pueblo_paleta", "Vecino de Pueblo Paleta", "youngster_lancere_0025",
                "La Ruta 1 está al norte. Toma esta Poción; puede salvarte en tu primer combate.",
                "Las Poképaradas azules vuelven a entregar objetos después de un tiempo.", "gift:pallet_potion", false);

        npc("dependiente_verde", "ciudad_verde", "Dependiente del Poké Mart", "gentleman_arthur_01a6",
                "Tengo un paquete reservado para el Profesor Oak. ¿Puedes llevárselo a Pueblo Paleta?",
                "El Profesor Oak está esperando su paquete en Pueblo Paleta.", "parcel", false);
        npc("enfermera_verde", "ciudad_verde", "Enfermera de Ciudad Verde", "beauty_grace_0111",
                "Bienvenido al Centro Pokémon de Ciudad Verde. Puedo recuperar a todo tu equipo.",
                "Tu equipo está en buenas manos.", "heal", true);
        npc("guardia_giovanni", "ciudad_verde", "Encargado del Gimnasio", "gentleman_brooks_01e2",
                "El líder Giovanni no se encuentra. Este gimnasio abrirá cuando hayas conseguido las otras siete medallas.",
                "Giovanni continúa ausente. Regresa con siete medallas.", "giovanni_gate", false);

        npc("cientifico_monte_luna", "monte_luna", "Investigador de fósiles", "scientist_shaun_0453",
                "Team Rocket buscaba fósiles en estas cavernas. Hemos recuperado dos; puedes salvar uno de ellos.",
                "Protege bien el fósil que elegiste.", "fossil", false);

        npc("enfermera_plateada", "ciudad_plateada", "Enfermera de Ciudad Plateada", "beauty_lola_010c",
                "El gimnasio de Brock pone a prueba la preparación de los entrenadores. Primero curaremos a tu equipo.",
                "Puedo curar a tu equipo antes o después del gimnasio.", "heal", true);
        npc("guia_plateada", "ciudad_plateada", "Guía del Gimnasio Roca", "hiker_nicholas_02f5",
                "Brock utiliza Pokémon de tipo Roca. Los ataques de Agua y Planta pueden darte ventaja.",
                "Observa el terreno y no dependas de un solo Pokémon.", "", false);

        npc("bill", "cabo_celeste", "Bill", "scientist_travon_0577",
                "¡Gracias por llegar hasta aquí! Necesito ayuda con un experimento. Después te entregaré un pase para el S.S. Anne.",
                "El S.S. Anne está atracado en Ciudad Carmín. Enseña tu pase en la entrada.", "bill", false);
        npc("enfermera_celeste", "ciudad_celeste", "Enfermera de Ciudad Celeste", "beauty_olivia_0112",
                "Los entrenadores de Ciudad Celeste dominan el tipo Agua. Recuperaré a tu equipo.",
                "Tu equipo vuelve a estar preparado.", "heal", true);
        npc("guia_celeste", "ciudad_celeste", "Guía del Gimnasio Cascada", "picnicker_diana_03e3",
                "Misty es rápida y agresiva. Los Pokémon Eléctricos o de Planta serán de gran ayuda.",
                "No subestimes la velocidad de Starmie.", "", false);

        npc("capitan_anne", "ss_anne", "Capitán del S.S. Anne", "sailor_paul_0340",
                "Has recorrido todo el barco y demostrado tu valor. Ya puedes continuar hacia el gimnasio de Ciudad Carmín.",
                "Buena suerte contra el Teniente Surge.", "ss_anne", false);
        npc("enfermera_carmin", "ciudad_carmin", "Enfermera de Ciudad Carmín", "beauty_sheila_010d",
                "Los combates del puerto pueden ser agotadores. Deja que cure a tu equipo.",
                "Tu equipo está listo para navegar o combatir.", "heal", true);
        npc("presidente_fanclub", "ciudad_carmin", "Presidente del Club Pokémon", "gentleman_walter_01a9",
                "Los Pokémon no son solo fuerza. Cuanto mejor los conozcas, mejor combatirán junto a ti.",
                "Cuida a tus compañeros y ellos cuidarán de ti.", "gift:fanclub", false);

        npc("informante_rocket", "ciudad_azulona", "Informante de Azulona", "gambler_rich_0108",
                "El cartel del casino esconde algo. Team Rocket utiliza un acceso secreto bajo la ciudad.",
                "Busca el mecanismo oculto en el casino.", "rocket_hideout", false);
        npc("enfermera_azulona", "ciudad_azulona", "Enfermera de Ciudad Azulona", "beauty_tamia_010a",
                "Ciudad Azulona es grande; descansa y recupera a tu equipo antes de explorarla.",
                "Tu equipo está completamente recuperado.", "heal", true);
        npc("anciana_te", "ciudad_azulona", "Anciana de Azulona", "lady_selphy_025e",
                "Los guardias de Ciudad Azafrán llevan horas trabajando. Esta bebida puede ayudarte a ganarte su confianza.",
                "Con paciencia, incluso las rutas cerradas terminan abriéndose.", "gift:tea", false);

        npc("senor_fuji", "pueblo_lavanda", "Señor Fuji", "old_sage",
                "La Torre Pokémon vuelve a estar en calma gracias a ti. Este objeto te permitirá despertar a los Pokémon dormidos del camino.",
                "El sonido de la Poké Flauta llega incluso a los corazones más dormidos.", "pokemon_tower", false);
        npc("enfermera_lavanda", "pueblo_lavanda", "Enfermera de Pueblo Lavanda", "channeler_rachel_0093",
                "La torre deja agotados a muchos equipos. Permíteme ayudarte.",
                "Tu equipo ha recuperado sus fuerzas.", "heal", true);

        npc("enfermera_fucsia", "ciudad_fucsia", "Enfermera de Ciudad Fucsia", "beauty_bridget_0109",
                "La Zona Safari y las rutas costeras exigen preparación. Curaré a tu equipo.",
                "Tu equipo está listo para continuar.", "heal", true);
        npc("guarda_safari", "ciudad_fucsia", "Guarda de la Zona Safari", "gentleman_tucker_01a7",
                "En la Zona Safari la paciencia importa más que la fuerza. Explora cada sendero.",
                "Respeta a los Pokémon y las normas de la reserva.", "gift:safari", false);

        npc("presidente_silph", "ciudad_azafran", "Presidente de Silph S.A.", "gentleman_norton_01a8",
                "Has liberado Silph S.A. de Team Rocket. Ahora el gimnasio de Sabrina puede reabrir.",
                "Sabrina te espera. Su poder psíquico pondrá a prueba tu estrategia.", "silph", false);
        npc("enfermera_azafran", "ciudad_azafran", "Enfermera de Ciudad Azafrán", "beauty_lori_010b",
                "Esta ciudad ha pasado por momentos difíciles. Tu equipo puede descansar aquí.",
                "Tu equipo vuelve a estar en plena forma.", "heal", true);

        npc("investigador_mansion", "isla_canela", "Investigador de la Mansión", "scientist_shaun_0551",
                "Entre los documentos de la mansión aparece una llave marcada con el símbolo del gimnasio.",
                "La llave abre el gimnasio de Blaine. Prepárate para temperaturas extremas.", "cinnabar_key", false);
        npc("enfermera_canela", "isla_canela", "Enfermera de Isla Canela", "beauty_cyndy_03f2",
                "El volcán y la mansión pueden debilitar rápidamente a un equipo. Yo me ocuparé de tus Pokémon.",
                "Tu equipo está listo para enfrentarse al fuego.", "heal", true);

        npc("recepcion_liga", "meseta_anil", "Recepcionista de la Liga", "ace_trainer_michelle_0258",
                "Las ocho medallas han sido verificadas. Calle Victoria y el Alto Mando están abiertos para ti.",
                "Tras esta puerta solo cuentan tu preparación y el vínculo con tu equipo.", "victory_road", false);
    }

    private static void registerRoutes() {
        route("ruta_1", "youngster_logan_02e8", "¡Mi primer combate en la Ruta 1 será contra ti!", "Seguiré entrenando cerca de Pueblo Paleta.");
        route("ruta_1", "lass_madeline_03dc", "Los Pokémon de esta ruta parecen tranquilos, pero nosotros no nos rendimos.", "Buen combate. Ciudad Verde está un poco más adelante.");

        route("bosque_verde", "bug_catcher_01ec", "¡Encontré un Pokémon increíble entre los árboles!", "Los Pokémon Bicho crecen muy rápido.");
        route("bosque_verde", "bug_catcher_anthony_0213", "En este bosque, quien baja la guardia termina rodeado.", "Has sabido abrirte paso.");
        route("bosque_verde", "bug_catcher_rick_0066", "¡Te vi entre las hojas! Prepárate para combatir.", "La salida norte ya está cerca.");
        route("bosque_verde", "bug_catcher_doug_0067", "Mis Pokémon conocen cada rincón de este bosque.", "Tu equipo se orienta mejor que el mío.");
        route("bosque_verde", "bug_catcher_sammy_0068", "Soy el último desafío antes de Ciudad Plateada.", "Brock será mucho más difícil que yo.");

        route("ruta_3", "youngster_jonathon_0393", "¡La Medalla Roca no te permitirá pasar sin un combate!", "Ahora entiendo por qué venciste a Brock.");
        route("ruta_3", "lass_janice_0074", "Los entrenadores de esta ruta venimos preparados.", "Monte Moon te espera al este.");
        route("ruta_3", "camper_liam_008e", "Acampé aquí para desafiar a quienes salieran de Ciudad Plateada.", "La próxima vez prepararé una estrategia mejor.");
        route("ruta_3", "hiker_daniel_02f4", "La montaña fortalece tanto a personas como a Pokémon.", "Avanza con cuidado dentro de la cueva.");
        route("ruta_3", "picnicker_siena_02f3", "Antes de descansar quiero un combate amistoso.", "Gracias. Ya podemos continuar el picnic.");

        route("monte_luna", "super_nerd_jovan_00a9", "Estos fósiles son demasiado valiosos para dejarlos pasar.", "Quizá la ciencia necesite mejores entrenadores.");
        route("monte_luna", "hiker_marcos_00b5", "Las cavernas son mi terreno. ¿Podrás vencerme aquí?", "Conoces bien las fortalezas de tu equipo.");
        route("monte_luna", "lass_iris_0079", "No dejaré que Team Rocket se apodere de la cueva.", "Parece que estamos del mismo lado.");
        route("monte_luna", "youngster_josh_005b", "Me perdí, pero encontré a alguien con quien combatir.", "Buscaré la salida contigo a la vista.");

        route("rutas_24_25", "camper_diego_0407", "El Puente Pepita reúne a entrenadores de todas partes.", "Has superado otra prueba del puente.");
        route("rutas_24_25", "lass_ali_007b", "Solo los mejores llegan hasta la casa de Bill.", "Parece que mereces conocerlo.");
        route("rutas_24_25", "hiker_justin_0307", "Este cabo tiene las mejores vistas y grandes combates.", "Disfruta del camino hasta Bill.");
        route("rutas_24_25", "picnicker_ana_0442", "¿Un combate antes de continuar hacia el cabo?", "Fue divertido. Sigue por el sendero.");
        route("rutas_24_25", "youngster_joey_005d", "Mi equipo ha mejorado desde la última ruta.", "El tuyo también se ha vuelto fuerte.");

        route("rutas_5_6", "camper_ethan_0090", "Ciudad Carmín queda al sur, pero primero tendrás que vencerme.", "La entrada subterránea está más adelante.");
        route("rutas_5_6", "bird_keeper_alexandra_0326", "Desde el cielo se ve todo el camino hasta el puerto.", "Tus Pokémon no perdieron de vista el combate.");
        route("rutas_5_6", "gentleman_jeremy_032d", "Un entrenador educado siempre acepta un desafío.", "Un combate excelente.");
        route("rutas_5_6", "camper_anthony_0304", "Llevo días entrenando junto a la entrada de la ciudad.", "Tendré que quedarme algunos días más.");

        route("ss_anne", "sailor_damian_03eb", "En alta mar hay que mantener el equilibrio incluso al combatir.", "Tienes buenas piernas de marinero.");
        route("ss_anne", "sailor_samson_0452", "¡Nadie recorre este barco sin probar mi equipo!", "Puedes continuar hacia la cubierta.");
        route("ss_anne", "gentleman_leonardo_0487", "Los pasajeros también sabemos combatir.", "Ha sido un duelo digno del salón principal.");
        route("ss_anne", "engineer_bernie_00de", "Mantengo las máquinas y entreno durante los descansos.", "Volveré al trabajo con nuevas ideas.");
        route("ss_anne", "picnicker_nancy_0097", "Este viaje necesitaba un poco de emoción.", "Gracias por el combate.");

        route("rutas_9_10", "hiker_franklin_01fe", "El camino al Túnel Roca no perdona a los equipos débiles.", "Tu equipo está preparado para la oscuridad.");
        route("rutas_9_10", "picnicker_hannah_00a1", "La central eléctrica hace que esta zona se sienta diferente.", "Quizá vuelva a explorar la central.");
        route("rutas_9_10", "camper_jeff_0092", "Entreno junto al río antes de entrar al túnel.", "Buen combate; vigila los desniveles.");
        route("rutas_9_10", "youngster_dave_0064", "¡No necesito linterna para ganar!", "Quizá sí necesite entrenar un poco más.");

        route("tunel_roca", "hiker_trent_0287", "Escucha el eco: te está avisando de nuestro combate.", "El eco ahora celebra tu victoria.");
        route("tunel_roca", "pokemaniac_ashton_00a8", "He estudiado cada Pokémon que habita estas cavernas.", "Todavía me quedan muchas estrategias por estudiar.");
        route("tunel_roca", "hiker_alan_00b9", "Las rocas muestran quién tiene verdadera resistencia.", "Tu equipo soportó toda la presión.");
        route("tunel_roca", "black_belt_david_038a", "Entreno en la oscuridad para agudizar mis sentidos.", "Sentí cada uno de tus movimientos.");

        route("torre_pokemon", "channeler_01c6", "Una presencia inquieta acompaña a tu equipo.", "La presencia se ha calmado.");
        route("torre_pokemon", "channeler_01c7", "Los espíritus desean comprobar tus intenciones.", "Puedes seguir ascendiendo.");
        route("torre_pokemon", "channeler_01c9", "Esta planta pertenece a quienes ya partieron.", "Tu respeto ha quedado demostrado.");
        route("torre_pokemon", "channeler_01cc", "No todos los misterios pueden resolverse con fuerza.", "Pero tu vínculo abrió el camino.");
        route("torre_pokemon", "channeler_01cd", "La cima está cerca, pero Team Rocket continúa allí.", "Libera al señor Fuji.");

        route("guarida_rocket", "scientist_emilio_048e", "Los experimentos de Team Rocket no pueden ser interrumpidos.", "Mis cálculos no contemplaban tu llegada.");
        route("guarida_rocket", "scientist_shaun_054f", "Has entrado en territorio de Team Rocket.", "No podrás detener a toda la organización.");
        route("guarida_rocket", "super_nerd_glenn_028a", "El ascensor está reservado para los jefes.", "La llave ya no está segura.");
        route("guarida_rocket", "scientist_shaun_0550", "Nuestros planes son más grandes que esta ciudad.", "Debo informar a Giovanni.");

        route("rutas_12_15", "fisherman_chip_00e2", "Esperar un Pokémon requiere la misma paciencia que un buen combate.", "Hoy tú fuiste la mejor captura.");
        route("rutas_12_15", "bird_keeper_benny_0296", "Mis Pokémon conocen todas las corrientes de aire de esta costa.", "Has cambiado el viento del combate.");
        route("rutas_12_15", "biker_jaren_0218", "Esta carretera es nuestra pista de entrenamiento.", "Tienes derecho a seguir rodando.");
        route("rutas_12_15", "swimmer_vanessa_0365", "No te fíes de un camino aparentemente tranquilo.", "Esta vez el sorprendido fui yo.");
        route("rutas_12_15", "swimmer_claire_0459", "La costa ofrece combates completamente distintos.", "Te adaptaste muy rápido.");

        route("silph", "scientist_beau_0154", "Silph ahora trabaja para Team Rocket.", "Quizá elegimos el bando equivocado.");
        route("silph", "scientist_ed_0158", "Los ascensores están bloqueados por orden de Giovanni.", "Tendrás que encontrar otra ruta.");
        route("silph", "scientist_jose_0152", "No llegarás hasta el presidente.", "No pude mantener la línea.");
        route("silph", "psychic_abigail_050b", "Team Rocket también reclutó especialistas psíquicos.", "Tu mente fue más firme.");
        route("silph", "scientist_connor_0150", "Este piso contiene investigación confidencial.", "La investigación volverá a sus propietarios.");

        route("rutas_16_18", "biker_lukas_0295", "El descenso desde Azulona pertenece a los ciclistas.", "Controlaste muy bien la velocidad.");
        route("rutas_16_18", "cue_ball_corey_02a7", "Aquí no hay guardias que detengan nuestro combate.", "Puedes continuar por la ciclovía.");
        route("rutas_16_18", "biker_ruben_02a3", "Cada curva es una oportunidad para desafiar a alguien.", "Ganaste esta carrera.");
        route("rutas_16_18", "biker_jaxon_02a5", "Dominar un equipo exige disciplina.", "Tus Pokémon confían plenamente en ti.");

        route("rutas_marinas", "swimmer_erik_035f", "¡Aquí no hay suelo firme que te ayude!", "Tu estrategia se mantuvo a flote.");
        route("rutas_marinas", "swimmer_haley_033c", "Las corrientes hacen cada combate impredecible.", "Supiste leer la corriente.");
        route("rutas_marinas", "swimmer_shelton_033a", "He nadado desde Isla Canela buscando rivales.", "El viaje valió la pena.");
        route("rutas_marinas", "swimmer_dillon_0364", "Mis Pokémon son más rápidos dentro del agua.", "Ni siquiera eso fue suficiente.");
        route("rutas_marinas", "swimmer_mary_033d", "Las Islas Espuma están más frías de lo que parecen.", "Prepárate antes de entrar.");

        route("islas_espuma", "swimmer_aubree_0375", "El hielo cambia por completo este combate.", "Has mantenido el equilibrio.");
        route("islas_espuma", "swimmer_cassandra_0378", "Solo los equipos resistentes atraviesan estas islas.", "El tuyo merece continuar.");
        route("islas_espuma", "ace_trainer_maya_0503", "Este lugar es perfecto para un entrenamiento avanzado.", "Tu técnica está al nivel de los mejores.");
        route("islas_espuma", "ace_trainer_dennis_04fe", "No esperes que el frío reduzca mi precisión.", "Tu equipo mantuvo la concentración.");

        route("mansion_canela", "scientist_fredrick_0576", "En estas ruinas todavía quedan tesoros sin dueño.", "Parece que esta vez no me llevaré nada.");
        route("mansion_canela", "scientist_taylor_0155", "Los documentos de este laboratorio no deben salir de aquí.", "La verdad ya no puede ocultarse.");
        route("mansion_canela", "scientist_travis_0159", "Los experimentos abandonados aún tienen valor.", "Será mejor evacuar el laboratorio.");
        route("mansion_canela", "scientist_jerry_0151", "La llave del gimnasio no está donde crees.", "Has aprendido a buscar bajo presión.");

        route("ruta_21", "swimmer_wesley_036e", "Pueblo Paleta está al norte, pero el mar todavía tiene desafíos.", "Has cerrado el círculo de tu viaje.");
        route("ruta_21", "swimmer_paige_0376", "Los entrenadores que vuelven de Isla Canela siempre son fuertes.", "Tú eres prueba de ello.");
        route("ruta_21", "swimmer_crystal_0377", "Esta corriente lleva directamente a tus recuerdos del inicio.", "Tu equipo ha cambiado muchísimo.");

        route("ruta_22", "ace_trainer_destiny_03b7", "Solo los dueños de ocho medallas pasan por aquí.", "Tu colección de medallas es auténtica.");
        route("ruta_22", "ace_trainer_zachery_03b3", "Calle Victoria empieza antes de cruzar la puerta.", "Estás preparado para entrar.");
        route("ruta_22", "bird_keeper_hana_038b", "Mis Pokémon vigilan el acceso desde el cielo.", "Tienes permiso para continuar.");

        route("calle_victoria", "black_belt_mike_013e", "La Liga solo acepta entrenadores que superan sus límites.", "Has roto otro de tus límites.");
        route("calle_victoria", "juggler_mason_024e", "Equilibrio, precisión y estrategia: necesitarás las tres.", "No perdiste el control ni un instante.");
        route("calle_victoria", "juggler_edward_0123", "Cada decisión equivocada se paga aquí.", "Elegiste correctamente.");
        route("calle_victoria", "black_belt_hugh_02b9", "Mi entrenamiento termina cuando encuentre a alguien digno.", "Hoy has terminado mi entrenamiento.");
        route("calle_victoria", "black_belt_shea_02b7", "El Alto Mando está cerca, pero yo no seré indulgente.", "Ya puedes ver la salida.");
        route("calle_victoria", "ace_trainer_henry_0383", "Esta es tu última prueba antes de la Liga.", "Ve y demuestra todo lo que aprendiste.");
    }

    private static void registerGyms() {
        gym(1, "Ciudad Plateada", "Brock", "kanto_brock", StoryStage.PARCEL_RETURNED, StoryStage.BROCK_DEFEATED,
                List.of("camper_curtis_03a3", "hiker_louis_03e0"));
        gym(2, "Ciudad Celeste", "Misty", "kanto_misty", StoryStage.BILL_HELPED, StoryStage.MISTY_DEFEATED,
                List.of("swimmerm_luis_00ea", "picnicker_isabelle_0098"));
        gym(3, "Ciudad Carmín", "Teniente Surge", "kanto_ltsurge", StoryStage.SS_ANNE_CLEARED, StoryStage.SURGE_DEFEATED,
                List.of("gentleman_thomas_01a5", "engineer_braxton_00dd", "engineer_baily_00dc"));
        gym(4, "Ciudad Azulona", "Erika", "kanto_erika", StoryStage.ROCKET_HIDEOUT_CLEARED, StoryStage.ERIKA_DEFEATED,
                List.of("beauty_devon_0457", "beauty_harley_0490", "picnicker_summer_0440"));
        gym(5, "Ciudad Fucsia", "Koga", "kanto_koga", StoryStage.POKEMON_TOWER_CLEARED, StoryStage.KOGA_DEFEATED,
                List.of("biker_jaren_028c", "cue_ball_chase_0101", "biker_ricardo_0217", "tamer_cole_0129"));
        gym(6, "Ciudad Azafrán", "Sabrina", "kanto_sabrina", StoryStage.SILPH_CO_CLEARED, StoryStage.SABRINA_DEFEATED,
                List.of("channeler_carly_01ba", "channeler_jennifer_01c5", "psychic_bryce_038c", "psychic_valencia_038d"));
        gym(7, "Isla Canela", "Blaine", "kanto_blaine", StoryStage.CINNABAR_KEY_FOUND, StoryStage.BLAINE_DEFEATED,
                List.of("scientist_parkker_0157", "scientist_gideon_0221", "scientist_joshua_0156", "scientist_rodney_0153"));
        gym(8, "Ciudad Verde", "Giovanni", "kanto_giovanni", StoryStage.BLAINE_DEFEATED, StoryStage.GIOVANNI_DEFEATED,
                List.of("black_belt_hideki_013f", "black_belt_hitoshi_0141", "ace_trainer_mariah_0384", "ace_trainer_omar_0382"));
    }

    private static void registerBosses() {
        boss("rival_paleta", "Rival", "rival_terry_0146", StoryStage.STARTER_CHOSEN, null,
                "Antes de salir de Pueblo Paleta quiero comprobar cuál de los dos eligió mejor.");
        boss("rival_celeste", "Rival", "rival_terry_014c", StoryStage.MOUNT_MOON_CLEARED, null,
                "Has llegado hasta Ciudad Celeste. Veamos cuánto ha mejorado tu equipo.");
        boss("rival_ss_anne", "Rival", "rival_terry_01aa", StoryStage.MISTY_DEFEATED, null,
                "Este barco está lleno de entrenadores, pero yo sigo siendo tu rival más importante.");
        boss("giovanni_azulona", "Giovanni", "kanto_giovanni", StoryStage.SURGE_DEFEATED, StoryStage.ROCKET_HIDEOUT_CLEARED,
                "Has arruinado demasiados planes de Team Rocket. Yo mismo terminaré este combate.");
        boss("rival_silph", "Rival", "rival_terry_01ad", StoryStage.KOGA_DEFEATED, null,
                "Incluso con Team Rocket ocupando el edificio, no pienso dejar pasar este combate.");
        boss("giovanni_silph", "Giovanni", "kanto_giovanni", StoryStage.KOGA_DEFEATED, StoryStage.SILPH_CO_CLEARED,
                "Silph S.A. pertenecerá a Team Rocket. Si quieres impedirlo, tendrás que vencerme otra vez.");
        boss("rival_liga", "Rival", "rival_terry_01b0", StoryStage.GIOVANNI_DEFEATED, null,
                "Nos acercamos al final. No entrarás a Calle Victoria sin enfrentarme.");
    }

    private static void registerLeague() {
        LEAGUE.add(new LeagueDefinition(1, "Lorelei", "kanto_league_lorelei", StoryStage.VICTORY_ROAD_CLEARED, StoryStage.LORELEI_DEFEATED));
        LEAGUE.add(new LeagueDefinition(2, "Bruno", "kanto_league_bruno", StoryStage.LORELEI_DEFEATED, StoryStage.BRUNO_DEFEATED));
        LEAGUE.add(new LeagueDefinition(3, "Agatha", "kanto_league_agatha", StoryStage.BRUNO_DEFEATED, StoryStage.AGATHA_DEFEATED));
        LEAGUE.add(new LeagueDefinition(4, "Lance", "kanto_league_lance", StoryStage.AGATHA_DEFEATED, StoryStage.LANCE_DEFEATED));
        LEAGUE.add(new LeagueDefinition(5, "Campeón Blue", "kanto_champion_blue", StoryStage.LANCE_DEFEATED, StoryStage.CHAMPION_DEFEATED));
    }

    private static void npc(String id, String zone, String name, String trainerId, String intro,
                            String repeat, String action, boolean nurse) {
        NPCS.put(id, new NpcDefinition(id, zone, name, trainerId, intro, repeat, action, nurse));
    }

    private static void route(String zone, String trainerId, String intro, String defeated) {
        List<TrainerDefinition> entries = ROUTES.computeIfAbsent(zone, ignored -> new ArrayList<>());
        entries.add(new TrainerDefinition(zone + "_" + (entries.size() + 1), zone, trainerId, intro, defeated));
    }

    private static void gym(int number, String city, String leaderName, String leaderId,
                            StoryStage required, StoryStage result, List<String> trainerIds) {
        List<TrainerDefinition> trainers = new ArrayList<>();
        for (int i = 0; i < trainerIds.size(); i++) {
            String id = "gimnasio_" + number + "_entrenador_" + (i + 1);
            trainers.add(new TrainerDefinition(id, "gimnasio_" + number, trainerIds.get(i),
                    "Antes de llegar al líder tendrás que superar mi desafío.",
                    "El líder te espera. Has demostrado que mereces enfrentarlo."));
        }
        GYMS.put(number, new GymDefinition(number, city, leaderName, leaderId, required, result, List.copyOf(trainers)));
    }

    private static void boss(String id, String name, String trainerId, StoryStage required,
                             StoryStage result, String intro) {
        BOSSES.put(id, new BossDefinition(id, name, trainerId, required, result, intro));
    }

    public static Optional<NpcDefinition> npc(String id) {
        return Optional.ofNullable(NPCS.get(normalize(id)));
    }

    public static Collection<NpcDefinition> npcs() {
        return List.copyOf(NPCS.values());
    }

    public static List<TrainerDefinition> route(String zone) {
        return ROUTES.getOrDefault(normalize(zone), List.of());
    }

    public static Collection<String> routeNames() {
        return List.copyOf(ROUTES.keySet());
    }

    public static Optional<GymDefinition> gym(int number) {
        return Optional.ofNullable(GYMS.get(number));
    }

    public static Collection<GymDefinition> gyms() {
        return List.copyOf(GYMS.values());
    }

    public static Optional<BossDefinition> boss(String id) {
        return Optional.ofNullable(BOSSES.get(normalize(id)));
    }

    public static Collection<BossDefinition> bosses() {
        return List.copyOf(BOSSES.values());
    }

    public static Optional<LeagueDefinition> league(int number) {
        return LEAGUE.stream().filter(entry -> entry.number() == number).findFirst();
    }

    public static List<LeagueDefinition> league() {
        return List.copyOf(LEAGUE);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replace(' ', '_').replace('-', '_');
    }

    public record NpcDefinition(String id, String zone, String name, String trainerId,
                                String intro, String repeat, String action, boolean nurse) {}

    public record TrainerDefinition(String id, String zone, String trainerId,
                                    String intro, String defeated) {}

    public record GymDefinition(int number, String city, String leaderName, String leaderTrainerId,
                                StoryStage requiredStage, StoryStage resultStage,
                                List<TrainerDefinition> trainers) {}

    public record BossDefinition(String id, String name, String trainerId, StoryStage requiredStage,
                                 StoryStage resultStage, String intro) {}

    public record LeagueDefinition(int number, String name, String trainerId, StoryStage requiredStage,
                                   StoryStage resultStage) {
        public boolean champion() {
            return number == 5;
        }
    }
}
