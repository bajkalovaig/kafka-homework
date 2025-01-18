using Npgsql;

const int MAX_PRESSURE_THRESHOLD = 78; // бар
const int MIN_PRESSURE_THRESHOLD = 38; // бар
var invariant = System.Globalization.CultureInfo.InvariantCulture;
var connectionString = "Host=localhost;Username=postgres;Password=password;Database=postgres";
var rnd = new Random();
await using (var dataSource = NpgsqlDataSource.Create(connectionString)) {
    //cmd.Parameters.AddWithValue("");
    while (true)
    {
        using var good = dataSource.CreateCommand($"INSERT INTO events (veh_id, sensor_value) VALUES ('1', {(rnd.Next(MIN_PRESSURE_THRESHOLD, MAX_PRESSURE_THRESHOLD) / 10.0).ToString(invariant)})");
        using var bad = dataSource.CreateCommand($"INSERT INTO events (veh_id, sensor_value) VALUES ('2', {(rnd.Next(MAX_PRESSURE_THRESHOLD, 150) / 10.0).ToString(invariant)})");
        using var random = dataSource.CreateCommand($"INSERT INTO events (veh_id, sensor_value) VALUES ('3', {(rnd.Next(0, 100) / 10.0).ToString(invariant)})");
        await good.ExecuteNonQueryAsync();
        await bad.ExecuteNonQueryAsync();
        await random.ExecuteNonQueryAsync();
        Thread.Sleep(TimeSpan.FromSeconds(1));
    }
    
};