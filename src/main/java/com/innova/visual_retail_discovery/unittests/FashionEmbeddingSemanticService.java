package com.innova.visual_retail_discovery.unittests;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.innova.visual_retail_discovery.model.SearchResult;

import java.io.IOException;
import java.util.*;

/**
 * Fashion App — Text Embedding Pipeline
 *
 * Two core features:
 *   1. Semantic Product Search  — match natural-language search queries to products
 *   2. Outfit Recommendations   — find similar / complementary items to what the user is viewing
 *
 * Only search queries are embedded at query-time.
 * Product embeddings are pre-computed once and cached (simulated here with a List).
 */
public class FashionEmbeddingSemanticService {

    // ── Model (singleton — load once at app startup) ─────────────────────────
    private final ZooModel<String, float[]> model;
    private final Predictor<String, float[]> predictor;

    // ── Simulated product catalog ─────────────────────────────────────────────
    private static final List<Product> CATALOG = List.of(
            new Product("P001",  "animal-textured-wallet by Accessorize",                                   List.of("wallet", "accessories"),              "datastore/animal-textured-wallet_Accessorize_Wallet_1090.jpg"),
            new Product("P002",  "Back Formal Blazer by PeterEngland",                                      List.of("blazer", "business formals", "workwear"), "datastore/Back Formal Blazer_PeterEngland_Business Formals_3000.jpg"),
            new Product("P003",  "Black Blazer by ManQ",                                                    List.of("blazer", "business formals", "workwear"), "datastore/Black Blazer_ManQ_Business Formals_2400.jpg"),
            new Product("P004",  "Black men faux leather jacket by Raxedo",                                 List.of("jacket", "outerwear", "casual"),       "datastore/Black men-fauz-leather-jacket_Raxedo_Jacket_1999.jpg"),
            new Product("P005",  "Black Mens Cap by LosAngles",                                             List.of("cap", "accessories"),                 "datastore/Black Mens Cap_LosAngles_Cap_500 .jpg"),
            new Product("P006",  "Black Denim Jacket",                                                      List.of("jacket", "denim", "casual"),           "datastore/Black-Jacket_Denim_Jacket_1500.jpg"),
            new Product("P007",  "Black Jeans by VanHeusen",                                                List.of("jeans", "bottoms", "casual"),          "datastore/Black-Jeans_VanHeusen _Jeans_2000.jpg"),
            new Product("P008",  "Black T-Shirt by GAP",                                                    List.of("t-shirt", "tops", "casual"),           "datastore/Black-Shirt_GAP_T-Shirt_1499.jpg"),
            new Product("P009",  "Black Shoes by Nike",                                                     List.of("shoes", "footwear", "sports"),         "datastore/Black-Shoes_Nike_Shoes_4560.jpg"),
            new Product("P010",  "Black Shoes by Puma",                                                     List.of("shoes", "footwear", "sports"),         "datastore/Black-Shoes_Puma_Shoes_300.jpg"),
            new Product("P011",  "Blue Shirt by Snitch",                                                    List.of("shirt", "tops", "casual"),             "datastore/Blue Shirt_Snitch_Shirt_999.jpg"),
            new Product("P012",  "Blue Hoodie by Peter England",                                            List.of("hoodie", "tops", "casual"),            "datastore/Blue-hoddy_peter-england_Hoddy_500.jpg"),
            new Product("P013",  "Blue Jacket by Levis",                                                    List.of("jacket", "outerwear", "casual"),       "datastore/Blue-Jacket_Levis_Jacket_7800.jpg"),
            new Product("P014",  "Blue T-Shirt by XY",                                                      List.of("t-shirt", "tops", "casual"),           "datastore/Blue-shirt_XY_T-Shirt_584.jpg"),
            new Product("P015",  "Brown T-Shirt by Tom Milton",                                             List.of("t-shirt", "tops", "casual"),           "datastore/Brown-T Shirt_Tom-Milton_T-Shirt_497.jpg"),
            new Product("P016",  "Buggy Jeans by FCUK",                                                     List.of("jeans", "bottoms", "casual"),          "datastore/Buggy Jeans_FCUK_Jeans_2499.jpg"),
            new Product("P017",  "Cotton Jeans by Bewakoof",                                                List.of("jeans", "bottoms", "casual"),          "datastore/Cotton Jeans_Bewakoof_Jeans_1499.jpg"),
            new Product("P018",  "Cotton comfort drawstring Trousers by Moda Rapido",                       List.of("trousers", "bottoms", "casual"),       "datastore/cotton-comfort-drastring_Moda-Rapido_Trousers_629.jpg"),
            new Product("P019",  "Cream Formal Blazer by Wyre",                                             List.of("blazer", "business formals", "workwear"), "datastore/Cream Formal Blazer_Wyre_Business Formals_2700.jpg"),
            new Product("P020",  "Cream Hoodie by Lenin",                                                   List.of("hoodie", "tops", "casual"),            "datastore/Cream-Hoddy_Lenin_hoddy_1200.jpg"),
            new Product("P021",  "Cream White Shirt by PeterEngland",                                       List.of("shirt", "tops", "casual"),             "datastore/Cream-white-Shirt_PeterEngland_Shirt_1200.jpg"),
            new Product("P022",  "Denim Jeans Jacket by GAP",                                               List.of("jacket", "denim", "casual"),           "datastore/Denim-Jeans-jacket_GAP_Jacket_1999.jpg"),
            new Product("P023",  "Denim Jeans Jacket by Rare Rabbit",                                       List.of("jacket", "denim", "casual"),           "datastore/Denim-Jeans-jacket_RARE-rabbit_Jacket_1999.jpg"),
            new Product("P024",  "Green Blazer by Invictus",                                                List.of("blazer", "business formals", "workwear"), "datastore/Green Blazer_Invictus_Business Formals_2800.jpg"),
            new Product("P025",  "Green Mens Cap by Jack And Jones",                                        List.of("cap", "accessories"),                 "datastore/Green Mens Cap_Jack And Jones_Cap_550 .jpg"),
            new Product("P026",  "Green Shirt by Roadster",                                                 List.of("shirt", "tops", "casual"),             "datastore/Green Shirt_Roadster_Shirt_621.jpg"),
            new Product("P027",  "Hand Bag by Fastrack",                                                    List.of("handbag", "accessories"),              "datastore/Hand-bag_fasttrack_HandBag_4560.jpg"),
            new Product("P028",  "Hand Bag by Trends",                                                      List.of("handbag", "accessories"),              "datastore/Hand-bag_trends_HandBag_6560.jpg"),
            new Product("P029",  "Vintage Hand Bag",                                                        List.of("handbag", "accessories"),              "datastore/Hand-Bag_Vintage_HandBag_4500.jpg"),
            new Product("P030",  "Hand Bag by Voyage",                                                      List.of("handbag", "accessories"),              "datastore/Hand-bag_voyage_HandBag_4560.jpg"),
            new Product("P031",  "Women Wallet Holder by Puma",                                             List.of("wallet", "accessories"),              "datastore/Holder_puma_Woment Wallet_798.jpg"),
            new Product("P032",  "Jacket by Westend",                                                       List.of("jacket", "outerwear", "casual"),       "datastore/Jacket_Westend_Jacket_4560.jpg"),
            new Product("P033",  "Jeans by Pepe Jeans",                                                     List.of("jeans", "bottoms", "casual"),          "datastore/Jeans_Pepe-jeans_Jeans_1500.jpg"),
            new Product("P034",  "Jeans by Wrgon",                                                          List.of("jeans", "bottoms", "casual"),          "datastore/Jeans_Wrgon_Jeans_1200.jpg"),
            new Product("P035",  "Jeans by Zodio",                                                          List.of("jeans", "bottoms", "casual"),          "datastore/Jeans_Zodio_Jeans_1111.jpg"),
            new Product("P036",  "Jeans Jacket by GAP",                                                     List.of("jacket", "denim", "casual"),           "datastore/Jeans-jacket_GAP_Jacket_2999.jpg"),
            new Product("P037",  "Kurtha by Volva",                                                         List.of("kurtha", "ethnic", "traditional"),     "datastore/Kurtha_Volva_kurtha_500.jpg"),
            new Product("P038",  "Maroon Shirt by Snitch",                                                  List.of("shirt", "tops", "casual"),             "datastore/Maroon Shirt_Snitch_Shirt_1119.jpg"),
            new Product("P039",  "Men Analogue Watch by Quartz",                                            List.of("watch", "accessories"),               "datastore/Men Analogue Watch_Quartz_Watches_2500.jpg"),
            new Product("P040",  "Men Green T-Shirt by Tommy",                                              List.of("t-shirt", "tops", "casual"),           "datastore/Men Green t-shirt_Tommy_T-Shirt_800.jpg"),
            new Product("P041",  "Men Silver Watch by Quartz",                                              List.of("watch", "accessories"),               "datastore/Men Silver Watch_Quartz_Watches_4000.jpg"),
            new Product("P042",  "Men Watch by Armani",                                                     List.of("watch", "accessories", "luxury"),     "datastore/Men Watch_armani_Watches_150000.jpg"),
            new Product("P043",  "Men Watch by Fords",                                                      List.of("watch", "accessories"),               "datastore/Men watch_fords_Watches_9000.jpg"),
            new Product("P044",  "Men Watch by Fossil",                                                     List.of("watch", "accessories"),               "datastore/Men watch_Fossil_Watches_9000.jpg"),
            new Product("P045",  "Men leather wallet by Allen Solly - 649",                                 List.of("wallet", "accessories"),              "datastore/men-leather-wallet_Allen-solly_Wallet_649.jpg"),
            new Product("P046",  "Men leather wallet by Allen Solly - 899",                                 List.of("wallet", "accessories"),              "datastore/men-leather-wallet_Allen-solly_Wallet_899.jpg"),
            new Product("P047",  "Men leather wallet by Rare Rabbit",                                       List.of("wallet", "accessories"),              "datastore/men-leather-wallet_Rare Rabbit_Wallet_1499.jpg"),
            new Product("P048",  "Men leather wallet by Van Heusen",                                        List.of("wallet", "accessories"),              "datastore/men-leather-wallet_van heusen_Wallet_1499.jpg"),
            new Product("P049",  "Mens Cap by LosAngles",                                                   List.of("cap", "accessories"),                 "datastore/Mens Cap_LosAngles_Cap_400 .jpg"),
            new Product("P050",  "Nehru Jacket by Anouk",                                                   List.of("jacket", "ethnic", "traditional"),    "datastore/Nehru-jacket_Anouk_Jacket_999.jpg"),
            new Product("P051",  "Nehru Jacket by Kaddi",                                                   List.of("jacket", "ethnic", "traditional"),    "datastore/Nehru-jacket_Kaddi_Jacket_999.jpg"),
            new Product("P052",  "Orange T-Shirt by Levis",                                                 List.of("t-shirt", "tops", "casual"),           "datastore/Orange-shirt_Levis_T-Shirt_1000.jpg"),
            new Product("P053",  "Pink Hoodie by Allen Solly",                                              List.of("hoodie", "tops", "casual"),            "datastore/pink-hoddy_Allen-solly_hoddy_2000.jpg"),
            new Product("P054",  "Plus A Line Top by Glitchez",                                             List.of("top", "tops", "casual"),              "datastore/Pluz A line Top_Glitchez_Top_874.jpg"),
            new Product("P055",  "Plus A Line Top by Sztori",                                               List.of("top", "tops", "casual"),              "datastore/Pluz A line Top_Sztori_Top_874.jpg"),
            new Product("P056",  "Printed Top by H&M",                                                      List.of("top", "tops", "casual"),              "datastore/Printed Tops_H & M_Top_299.jpg"),
            new Product("P057",  "Rain Coat by H&M - 1600",                                                 List.of("rain suit", "outerwear", "casual"),    "datastore/rain-coat_h&M_Rain-Suite_1600.jpg"),
            new Product("P058",  "Rain Coat by H&M - 1605",                                                 List.of("rain suit", "outerwear", "casual"),    "datastore/rain-coat_h&M_Rain-Suite_1605.jpg"),
            new Product("P059",  "Rapid Dry Jacket by HRX",                                                 List.of("jacket", "outerwear", "sports"),       "datastore/rapid-dry-jacket_HRX_Jacket_779.jpg"),
            new Product("P060",  "Red Casual Shoes by Nike",                                                List.of("shoes", "footwear", "sports"),         "datastore/Red Casual-Shoes_Nike_Shoes_1300.jpg"),
            new Product("P061",  "Red Mens Cap by Jack And Jones",                                          List.of("cap", "accessories"),                 "datastore/Red Mens Cap_Jack And Jones_Cap_650 .jpg"),
            new Product("P062",  "Red T-Shirt by Jockey",                                                   List.of("t-shirt", "tops", "casual"),           "datastore/red-shirt_Jockey_T-Shirt_100.jpg"),
            new Product("P063",  "Red T-Shirt by Roadster",                                                 List.of("t-shirt", "tops", "casual"),           "datastore/red-shirt_Roadstr_T-Shirt_100.jpg"),
            new Product("P064",  "Sarees Set Kurta by Kalini",                                              List.of("kurta", "ethnic", "traditional"),     "datastore/Sarees Set_Kalini_Kurta_915.jpg"),
            new Product("P065",  "Shirt by Allen Solly",                                                    List.of("shirt", "tops", "casual"),             "datastore/Shirt_Allen-solly_Shirt_2000.jpg"),
            new Product("P066",  "Shirt by Lloyds",                                                         List.of("shirt", "tops", "casual"),             "datastore/Shirt_Lloyds_shirt_1100.jpg"),
            new Product("P067",  "Shirt by Mast & Harbour",                                                 List.of("shirt", "tops", "casual"),             "datastore/Shirt_mast&Harbour_Shirt_1300.jpg"),
            new Product("P068",  "Shirt by Roadster - 556",                                                 List.of("shirt", "tops", "casual"),             "datastore/Shirt_Roadster_Shirt_556.jpg"),
            new Product("P069",  "Shirt by Roadster - 600",                                                 List.of("shirt", "tops", "casual"),             "datastore/Shirt_Roadster_Shirt_600.jpg"),
            new Product("P070",  "Shirt by Snitch - 1300",                                                  List.of("shirt", "tops", "casual"),             "datastore/Shirt_Snitch_Shirt_1300.jpg"),
            new Product("P071",  "Shirt by Snitch - 1400",                                                  List.of("shirt", "tops", "casual"),             "datastore/Shirt_Snitch_Shirt_1400.jpg"),
            new Product("P072",  "Shirt by Snitch - 1409",                                                  List.of("shirt", "tops", "casual"),             "datastore/Shirt_Snitch_Shirt_1409.jpg"),
            new Product("P073",  "Shirt by Snitch - 2443",                                                  List.of("shirt", "tops", "casual"),             "datastore/Shirt_Snitch_Shirt_2443.jpg"),
            new Product("P074",  "Top by Tokyo Talkies (Shirt)",                                            List.of("top", "tops", "casual"),              "datastore/Shirt_Top_Tokyo Talkies_Top_405.jpg"),
            new Product("P075",  "Shirt by USPA",                                                           List.of("shirt", "tops", "casual"),             "datastore/shirt_USPA_SHIRT_4560.jpg"),
            new Product("P076",  "Slim Fit Jeans by Highlander",                                            List.of("jeans", "bottoms", "casual"),          "datastore/slim fit jeans_Highlander_Jeans_899.jpg"),
            new Product("P077",  "Slim Fit Jeans by Nautica",                                               List.of("jeans", "bottoms", "casual"),          "datastore/slim fit jeans_Nautica_Jeans_899.jpg"),
            new Product("P078",  "Slim Fit Jeans by US Polo - 1899",                                        List.of("jeans", "bottoms", "casual"),          "datastore/slim fit jeans_US POLO_Jeans_1899.jpg"),
            new Product("P079",  "Slim Fit Jeans by US Polo - 2899",                                        List.of("jeans", "bottoms", "casual"),          "datastore/slim fit jeans_US POLO_Jeans_2899.jpg"),
            new Product("P080",  "Slim Fit Jeans by US Polo - 899",                                         List.of("jeans", "bottoms", "casual"),          "datastore/slim fit jeans_US POLO_Jeans_899.jpg"),
            new Product("P081",  "Specs Frame Goggles by Lenskart - 500",                                   List.of("goggles", "eyewear", "accessories"),  "datastore/specs-frame_lenskart_Goggles_500.jpg"),
            new Product("P082",  "Specs Frame Goggles by Lenskart - 700",                                   List.of("goggles", "eyewear", "accessories"),  "datastore/specs-frame_lenskart_Goggles_700.jpg"),
            new Product("P083",  "Sporty Jacket by TechnoSport",                                            List.of("jacket", "outerwear", "sports"),       "datastore/Sporty-Jacket_TechnoSport_Jacket_829.jpg"),
            new Product("P084",  "Straight Jeans by Highlander",                                            List.of("jeans", "bottoms", "casual"),          "datastore/Straight jeans_Highlander_Jeans_679.jpg"),
            new Product("P085",  "Stretchable Jeans by H&M",                                                List.of("jeans", "bottoms", "casual"),          "datastore/Strechtable jeans_H&M_Jeans_999.jpg"),
            new Product("P086",  "Stretchable Jeans by Highlander",                                         List.of("jeans", "bottoms", "casual"),          "datastore/Strechtable jeans_Highlander_Jeans_999.jpg"),
            new Product("P087",  "Stretchable Jeans by I&Gco",                                              List.of("jeans", "bottoms", "casual"),          "datastore/Strechtable jeans_I&Gco_Jeans_999.jpg"),
            new Product("P088",  "Stretchable Jeans by Roadster",                                           List.of("jeans", "bottoms", "casual"),          "datastore/Strechtable jeans_Roadster_Jeans_999.jpg"),
            new Product("P089",  "Stretchable Jeans by WRONG",                                              List.of("jeans", "bottoms", "casual"),          "datastore/Strechtable jeans_WRONG_Jeans_999.jpg"),
            new Product("P090",  "Striped Bomber Jacket by Glitchez",                                       List.of("jacket", "outerwear", "casual"),       "datastore/striped-bomber-jocket_Glitchez_Jacket_637.jpg"),
            new Product("P091",  "Sun Goggles by Raymon",                                                   List.of("goggles", "eyewear", "accessories"),  "datastore/sun-goggles_raymon_Goggles_4500.jpg"),
            new Product("P092",  "Sun Goggles by RaySun - 1500",                                            List.of("goggles", "eyewear", "accessories"),  "datastore/sun-goggles_raySun_Goggles_1500.jpg"),
            new Product("P093",  "Sun Goggles by RaySun - 2000",                                            List.of("goggles", "eyewear", "accessories"),  "datastore/sun-goggles_raySun_Goggles_2000.jpg"),
            new Product("P094",  "Sun Jacket by Blue Tyga",                                                 List.of("jacket", "outerwear", "casual"),       "datastore/Sun-jacket_Blue-tyga_Jacket_999.jpg"),
            new Product("P095",  "Sweat Shirt Hoodie by Lenin",                                             List.of("hoodie", "tops", "casual"),            "datastore/sweat-shirt_lenin_hoddy_1200.jpg"),
            new Product("P096",  "Three Fold Wallet by The Wallet Store",                                   List.of("wallet", "accessories"),              "datastore/three fold wallet_the wallet store_wallet_974.jpg"),
            new Product("P097",  "Top by Cutiekins",                                                        List.of("top", "tops", "casual"),              "datastore/Tops_Cutiekins_Top_429.jpg"),
            new Product("P098",  "Top by Glitchez",                                                         List.of("top", "tops", "casual"),              "datastore/Tops_Glitchez_Top_499.jpg"),
            new Product("P099",  "Top by H&M",                                                              List.of("top", "tops", "casual"),              "datastore/Tops_H & M_Top_690.jpg"),
            new Product("P100",  "Top by Jockey",                                                           List.of("top", "tops", "casual"),              "datastore/Tops_Jockey_Top_499.jpg"),
            new Product("P101",  "Top by Peach",                                                            List.of("top", "tops", "casual"),              "datastore/Tops_Peach_Top_459.jpg"),
            new Product("P102",  "Top by Phosphorus",                                                       List.of("top", "tops", "casual"),              "datastore/Tops_Phosphorus_Top_727.jpg"),
            new Product("P103",  "Top by Show Offff",                                                       List.of("top", "tops", "casual"),              "datastore/Tops_Show Offff_Top_600.jpg"),
            new Product("P104",  "Top by Tokyo Talkies",                                                    List.of("top", "tops", "casual"),              "datastore/Tops_Tokyo Talkies_Top_499.jpg"),
            new Product("P105",  "Track Pant by Nike",                                                      List.of("trousers", "bottoms", "sports"),       "datastore/Track-pant_nike_Jeans_1500.jpg"),
            new Product("P106",  "T-Shirt by Tommy",                                                        List.of("t-shirt", "tops", "casual"),           "datastore/t-shirt_Tommy_T-Shirt_400.jpg"),
            new Product("P107",  "T-Shirt by VK",                                                           List.of("t-shirt", "tops", "casual"),           "datastore/T-shirt_VK_T-Shirt_750.jpg"),
            new Product("P108",  "Unisex leather card holder by Puma",                                      List.of("wallet", "accessories"),              "datastore/unisex-leather-card holder_puma_Wallet_948.jpg"),
            new Product("P109",  "Unisex leather card holder by The Preppy",                                List.of("wallet", "accessories"),              "datastore/unisex-leather-card_holder-the preppy_Wallet_570.jpg"),
            new Product("P110",  "Unisex leather card holder by The Wallet Store",                          List.of("wallet", "accessories"),              "datastore/unisex-leather-card-holder_The wallet store_Wallet_948.jpg"),
            new Product("P111",  "Wallet by Calvin Klein",                                                  List.of("wallet", "accessories", "luxury"),    "datastore/Wallet_calvinKlein_Wallet_7800.jpg"),
            new Product("P112",  "Wallet by Nike",                                                          List.of("wallet", "accessories"),              "datastore/wallet_nike_Wallet_4500.jpg"),
            new Product("P113",  "Wallet by Steve Johnson - 1500",                                          List.of("wallet", "accessories"),              "datastore/wallet_steveJhonson_Wallet_1500.jpg"),
            new Product("P114",  "Wallet by Steve Johnson - 1600",                                          List.of("wallet", "accessories"),              "datastore/Wallet_SteveJhonson_Wallet_1600.jpg"),
            new Product("P115",  "White Blazer by Cantabil",                                                List.of("blazer", "business formals", "workwear"), "datastore/White Blazer_Cantabil_Business Formals_2400.jpg"),
            new Product("P116",  "White Cotton Jeans by Bene Kleed",                                        List.of("jeans", "bottoms", "casual"),          "datastore/White Cotton Jeans_Bene Kleed_Jeans_1499.jpg"),
            new Product("P117",  "White Mens Cap by Jack And Jones",                                        List.of("cap", "accessories"),                 "datastore/White Mens Cap_Jack And Jones_Cap_500 .jpg"),
            new Product("P118",  "White T-Shirt by Allen Solly",                                            List.of("t-shirt", "tops", "casual"),           "datastore/White T-shirt_Allen-solly_T-Shirt_450.jpg"),
            new Product("P119",  "White T-Shirt by Jockey",                                                 List.of("t-shirt", "tops", "casual"),           "datastore/white-shirt_Jockey_T-Shirt_799.jpg"),
            new Product("P120",  "White T-Shirt by Levis",                                                  List.of("t-shirt", "tops", "casual"),           "datastore/white-shirt_Levis_T-Shirt_400.jpg"),
            new Product("P121",  "White T-Shirt by Nike",                                                   List.of("t-shirt", "tops", "casual"),           "datastore/white-shirt_Nike_T-Shirt_750.jpg"),
            new Product("P122",  "White T-Shirt by XY",                                                     List.of("t-shirt", "tops", "casual"),           "datastore/white-shirt_XY_T-Shirt_400.jpg"),
            new Product("P123",  "White Shoes by Reebok - 1600",                                            List.of("shoes", "footwear", "sports"),         "datastore/White-shoes_Reebok_Shoes_1600.jpg"),
            new Product("P124",  "White Shoes by Reebok - 2500",                                            List.of("shoes", "footwear", "sports"),         "datastore/White-shoes_Reebok_Shoes_2500.jpg"),
            new Product("P125",  "Winter Jacket by GAP",                                                    List.of("jacket", "outerwear", "winter"),       "datastore/Winter-jacket_GAP_Jacket_1999.jpg"),
            new Product("P126",  "Women AirMelt Pants by BlissClub",                                        List.of("trousers", "bottoms", "casual"),       "datastore/Women AirMelt Pants_BlisscluB_Trousers_999.jpg"),
            new Product("P127",  "Women Blue Kurtha by Nike",                                               List.of("kurtha", "ethnic", "traditional"),    "datastore/Women Blue-Kurtha_nike_Kurtha_1200.jpg"),
            new Product("P128",  "Women Cotton Cargos Trousers by Roadster",                                List.of("trousers", "bottoms", "casual"),       "datastore/Women Cotton cargos Trousers_Roadster_Trousers_583.jpg"),
            new Product("P129",  "Women High Rise Trousers by Sassafras",                                   List.of("trousers", "bottoms", "casual"),       "datastore/women high rise Trousers_Sassafras_Trousers_791.jpg"),
            new Product("P130",  "Women High Rise Trousers by Stylecast X Slyck",                           List.of("trousers", "bottoms", "casual"),       "datastore/Women High Rise Trousers_Stylecast X Slyck_Trousers_575.jpg"),
            new Product("P131",  "Women Kurta Set by Libas - 1259",                                         List.of("kurta", "ethnic", "traditional"),     "datastore/Women Kurta Set_Libas_Kurta_1259.jpg"),
            new Product("P132",  "Women Kurta Set by Libas - 949",                                          List.of("kurta", "ethnic", "traditional"),     "datastore/Women Kurta Set_Libas_Kurta_949.jpg"),
            new Product("P133",  "Women Kurta Set by Sabil",                                                List.of("kurta", "ethnic", "traditional"),     "datastore/Women Kurta Set_Sabil_Kurta_949.jpg"),
            new Product("P134",  "Women Kurta Set by Sangria",                                              List.of("kurta", "ethnic", "traditional"),     "datastore/Women Kurta Set_Sangria_Kurta_949.jpg"),
            new Product("P135",  "Women Loose Fit Wide Leg Trousers by Style Cast X Revolte",               List.of("trousers", "bottoms", "casual"),       "datastore/Women Losse Fit Wide Leg_Style Cast X Revolte_Trousers_1117.jpg"),
            new Product("P136",  "Women Watch by Fords",                                                    List.of("watch", "accessories"),               "datastore/Women watch_fords_Watches_15000.jpg"),
            new Product("P137",  "Women Watch by Fors",                                                     List.of("watch", "accessories"),               "datastore/Women watch_fors_Watches_12000.jpg"),
            new Product("P138",  "Women Watch by Fossil",                                                   List.of("watch", "accessories"),               "datastore/Women watch_fossil_Watches_10000.jpg"),
            new Product("P139",  "Women Watch by Kors",                                                     List.of("watch", "accessories"),               "datastore/Women watch_kors_Watches_12000.jpg"),
            new Product("P140",  "Women Watch by More",                                                     List.of("watch", "accessories"),               "datastore/Women Watch_more_Watches_12500.jpg"),
            new Product("P141",  "Women White Kurtha by Highlander",                                        List.of("kurtha", "ethnic", "traditional"),    "datastore/Women white-kurtha_highlander_Kurtha_1200.jpg"),
            new Product("P142",  "Women Formal Parallel Trousers by Sassafras Worklyf",                     List.of("trousers", "bottoms", "workwear"),     "datastore/Women-fromal-parallel-Trousers_Sassafras-Worklyf_Trousers_1039.jpg"),
            new Product("P143",  "Women High Rise Trousers by BlissClub",                                   List.of("trousers", "bottoms", "casual"),       "datastore/Women-high-rice-trousers_BlissClub_Trousers_1039.jpg"),
            new Product("P144",  "Women Pleated Trousers by Kotty",                                         List.of("trousers", "bottoms", "casual"),       "datastore/women-pleated-trousers_Kotty_Trousers_499.jpg"),
            new Product("P145",  "Women Red Kurtha by Nyka",                                                List.of("kurtha", "ethnic", "traditional"),    "datastore/Women-red-kurtha_nyka_Kurtha_1200.jpg"),
            new Product("P146",  "Women Hoodie by Myntra",                                                  List.of("hoodie", "tops", "casual"),            "datastore/Woment Hoddy_Myntra_Women Hoddy_1200.jpg")
    );

    // Pre-computed product embeddings (in production: load from vector DB / cache)
    private final float[][] catalogEmbeds;

    // ─────────────────────────────────────────────────────────────────────────

    public FashionEmbeddingSemanticService()
            throws ModelNotFoundException, MalformedModelException, IOException, TranslateException {

        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();

        model     = criteria.loadModel();
        predictor = model.newPredictor();

        // Pre-compute & cache all product embeddings once at startup
        System.out.println("Indexing product catalog...");
        catalogEmbeds = new float[CATALOG.size()][];
        for (int i = 0; i < CATALOG.size(); i++) {
            catalogEmbeds[i] = predictor.predict(CATALOG.get(i).description);
        }
        System.out.printf("✓ %d products indexed.%n%n", CATALOG.size());
    }

    // ── Feature 1: Semantic Product Search ───────────────────────────────────

    /**
     * Embeds the user's search query at query-time, then ranks all products
     * by cosine similarity against the pre-computed catalog embeddings.
     *
     * @param query  Raw natural-language search string typed by the user
     * @param topN   How many results to return
     */
    public List<SearchResult> search(String query, int topN) throws TranslateException {
        float[] queryEmbed = predictor.predict(query);   // <-- only the query is embedded here

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < CATALOG.size(); i++) {
            Product p = CATALOG.get(i);
            results.add(new SearchResult(p.imagePath(), p.tags(), cosineSimilarity(queryEmbed, catalogEmbeds[i]), null, p.description(), "", 0.0));
        }
        results.sort(Comparator.comparingDouble((SearchResult r) -> r.score).reversed());
        return results.subList(0, Math.min(topN, results.size()));
    }

    // ── Feature 2: Outfit Similarity / "You May Also Like" ───────────────────

    /**
     * Given a product the user is currently viewing, finds the most semantically
     * similar items in the catalog (excluding the item itself).
     *
     * Useful for "Complete the Look" and "Similar Items" carousels.
     *
     * @param productId  ID of the product the user is viewing
     * @param topN       Number of recommendations to return
     */
    public List<SearchResult> findSimilar(String productId, int topN) throws TranslateException {
        int sourceIdx = indexById(productId);
        if (sourceIdx == -1) throw new IllegalArgumentException("Product not found: " + productId);

        float[] sourceEmbed = catalogEmbeds[sourceIdx];   // already cached — no re-embedding needed

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < CATALOG.size(); i++) {
            if (i == sourceIdx) continue;                  // skip the item itself
            Product p = CATALOG.get(i);
            long pid = Long.parseLong(p.id().substring(1));
            results.add(new SearchResult(p.id(), p.tags(), cosineSimilarity(sourceEmbed, catalogEmbeds[i]), pid, p.description(), "", 0.0));
        }
        results.sort(Comparator.comparingDouble((SearchResult r) -> r.score).reversed());
        return results.subList(0, Math.min(topN, results.size()));
    }

    private int indexById(String id) {
        for (int i = 0; i < CATALOG.size(); i++) {
            if (CATALOG.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    public void close() {
        predictor.close();
        model.close();
    }

    // ── Cosine similarity ─────────────────────────────────────────────────────

    static float cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10));
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    record Product(String id, String description, List<String> tags, String imagePath) {}


    // ── Demo main ─────────────────────────────────────────────────────────────

    public List<SearchResult> getSemanticResults(String query)
            throws ModelNotFoundException, MalformedModelException, IOException, TranslateException {

        FashionEmbeddingSemanticService svc = new FashionEmbeddingSemanticService();

        List<SearchResult> searchResults = svc.search(query, 3);

        return searchResults;
    }
}
