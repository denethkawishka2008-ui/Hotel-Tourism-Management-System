<?php
// --- PHP BACKEND HANDLER (Database Connection & API) ---
$db_host = 'localhost';
$db_user = 'root';
$db_pass = 'deneth';
$db_name = 'hospitality_db';

$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);

// Tables create karaganimata check kirima
if (!$conn->connect_error) {
    $conn->query("CREATE TABLE IF NOT EXISTS customer_feedback (
        id INT AUTO_INCREMENT PRIMARY KEY,
        customer_name VARCHAR(100),
        room_number VARCHAR(20),
        rating INT,
        comments TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )");

    $conn->query("CREATE TABLE IF NOT EXISTS food_orders (
        id INT AUTO_INCREMENT PRIMARY KEY,
        customer_name VARCHAR(100),
        room_number VARCHAR(20),
        food_item VARCHAR(150),
        quantity INT,
        special_request TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )");
}

// Form submissions handle kirima
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';

    if ($action === 'submit_feedback') {
        $name = $_POST['customer_name'] ?? 'Guest';
        $room = $_POST['room_number'] ?? 'N/A';
        $rating = (int)($_POST['rating'] ?? 5);
        $comments = $_POST['comments'] ?? '';

        $stmt = $conn->prepare("INSERT INTO customer_feedback (customer_name, room_number, rating, comments) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("ssis", $name, $room, $rating, $comments);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Feedback submitted successfully!"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Failed to save feedback."]);
        }
        exit;
    }

    if ($action === 'order_food') {
        $name = $_POST['customer_name'] ?? 'Guest';
        $room = $_POST['room_number'] ?? 'N/A';
        $item = $_POST['food_item'] ?? '';
        $qty = (int)($_POST['quantity'] ?? 1);
        $request = $_POST['special_request'] ?? '';

        $stmt = $conn->prepare("INSERT INTO food_orders (customer_name, room_number, food_item, quantity, special_request) VALUES (?, ?, ?, ?, ?)");
        $stmt->bind_param("sssis", $name, $room, $item, $qty, $request);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Food order placed successfully!"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Failed to place order."]);
        }
        exit;
    }
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DK Tourism Hotel - Guest Portal</title>
    <!-- jQuery CDN -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        :root {
            --primary: #d97706;
            --primary-dark: #b45309;
            --secondary: #2563eb;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text: #1e293b;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            margin: 0;
            padding: 20px;
        }

        .container {
            max-width: 700px;
            margin: 0 auto;
            background: var(--card-bg);
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.08);
        }

        h1, h2 {
            text-align: center;
            color: var(--primary-dark);
        }

        .section-box {
            background: #fffbeb;
            border: 1px solid #fde68a;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 25px;
        }

        /* Star Rating Hover & Active Animation */
        .star-rating {
            display: flex;
            justify-content: center;
            flex-direction: row-reverse;
            font-size: 40px;
            gap: 10px;
            margin: 15px 0;
        }

        .star-rating input {
            display: none;
        }

        .star-rating label {
            color: #cbd5e1;
            cursor: pointer;
            transition: color 0.2s ease, transform 0.2s ease;
        }

        .star-rating label:hover,
        .star-rating label:hover ~ label,
        .star-rating input:checked ~ label {
            color: #f59e0b;
            transform: scale(1.15);
        }

        .form-group {
            margin-bottom: 15px;
        }

        label {
            display: block;
            margin-bottom: 5px;
            font-weight: 600;
        }

        input[type="text"], input[type="number"], textarea, select {
            width: 100%;
            padding: 10px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            box-sizing: border-box;
            font-size: 14px;
        }

        button {
            width: 100%;
            background: var(--primary);
            color: white;
            border: none;
            padding: 12px;
            border-radius: 6px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            transition: background 0.3s;
        }

        button:hover {
            background: var(--primary-dark);
        }

        .alert {
            display: none;
            padding: 12px;
            border-radius: 6px;
            text-align: center;
            margin-top: 15px;
            font-weight: bold;
        }

        .alert-success { background: #dcfce7; color: #15803d; }
        .alert-error { background: #fee2e2; color: #b91c1c; }
    </style>
</head>
<body>

<div class="container">
    <h1>✨ DK Tourism Hotel Guest Portal ✨</h1>

    <!-- SECTION 1: 5-STAR RATING & FEEDBACK -->
    <div class="section-box">
        <h2>⭐ Rate Your Experience & Feedback</h2>
        <form id="feedbackForm">
            <div class="form-group">
                <label>Your Name:</label>
                <input type="text" name="customer_name" placeholder="Enter your name" required>
            </div>
            <div class="form-group">
                <label>Room Number:</label>
                <input type="text" name="room_number" placeholder="Room No" required>
            </div>

            <!-- Glowing Star Rating -->
            <div class="star-rating">
                <input type="radio" id="star5" name="rating" value="5" required/><label for="star5">★</label>
                <input type="radio" id="star4" name="rating" value="4"/><label for="star4">★</label>
                <input type="radio" id="star3" name="rating" value="3"/><label for="star3">★</label>
                <input type="radio" id="star2" name="rating" value="2"/><label for="star2">★</label>
                <input type="radio" id="star1" name="rating" value="1"/><label for="star1">★</label>
            </div>

            <div class="form-group">
                <label>Your Comments / Suggestions:</label>
                <textarea name="comments" rows="3" placeholder="Tell us about your stay..."></textarea>
            </div>
            <button type="submit">Submit Feedback</button>
            <div id="feedbackAlert" class="alert"></div>
        </form>
    </div>

    <!-- SECTION 2: FOOD MENU SEARCH & ORDERING -->
    <div class="section-box" style="background: #eff6ff; border-color: #bfdbfe;">
        <h2 style="color: #1d4ed8;">🍽️ Food & Dining Ordering</h2>
        <form id="foodOrderForm">
            <div class="form-group">
                <label>Select Food Item:</label>
                <select name="food_item" required>
                    <option value="">-- Choose Food Item --</option>
                    <option value="Sri Lankan Traditional Rice & Curry">Sri Lankan Traditional Rice & Curry</option>
                    <option value="Western & Continental Breakfast Platter">Western & Continental Breakfast Platter</option>
                    <option value="Fresh Seafood Platter & BBQ">Fresh Seafood Platter & BBQ</option>
                    <option value="Fresh Tropical Fruit Juice">Fresh Tropical Fruit Juice</option>
                    <option value="Devilled Chicken / Seafood Special">Devilled Chicken / Seafood Special</option>
                </select>
            </div>
            <div class="form-group">
                <label>Quantity:</label>
                <input type="number" name="quantity" value="1" min="1" required>
            </div>
            <div class="form-group">
                <label>Special Requests (e.g., Less spicy, no dairy):</label>
                <textarea name="special_request" rows="2" placeholder="Any special notes..."></textarea>
            </div>
            <button type="submit" style="background: var(--secondary);">Place Food Order</button>
            <div id="foodAlert" class="alert"></div>
        </form>
    </div>
</div>

<script>
$(document).ready(function() {

    // Handle Feedback Form Submission via AJAX
    $('#feedbackForm').on('submit', function(e) {
        e.preventDefault();
        var formData = $(this).serialize() + '&action=submit_feedback';

        $.ajax({
            url: '',
            type: 'POST',
            data: formData,
            dataType: 'json',
            success: function(response) {
                if (response.status === 'success') {
                    $('#feedbackAlert').removeClass('alert-error').addClass('alert-success').text(response.message).show();
                    $('#feedbackForm')[0].reset();
                } else {
                    $('#feedbackAlert').removeClass('alert-success').addClass('alert-error').text(response.message).show();
                }
            }
        });
    });

    // Handle Food Order Form Submission via AJAX
    $('#foodOrderForm').on('submit', function(e) {
        e.preventDefault();
        var formData = $(this).serialize() + '&action=order_food';

        $.ajax({
            url: '',
            type: 'POST',
            data: formData,
            dataType: 'json',
            success: function(response) {
                if (response.status === 'success') {
                    $('#foodAlert').removeClass('alert-error').addClass('alert-success').text(response.message).show();
                    $('#foodOrderForm')[0].reset();
                } else {
                    $('#foodAlert').removeClass('alert-success').addClass('alert-error').text(response.message).show();
                }
            }
        });
    });

});
</script>

</body>
</html>