package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.Connection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResetAdminPasswordTest {
    @Test void invalidInputNeverTouchesTheDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        assertThrows(IllegalArgumentException.class, () -> ResetAdminPassword.reset(connection, 1, "owner", "short".toCharArray(), "short".toCharArray()));
        verifyNoInteractions(connection);
    }
    @Test void existingTransactionIsNeverRepurposed() throws Exception {
        Connection connection = mock(Connection.class); when(connection.getAutoCommit()).thenReturn(false);
        char[] password = "Synthetic recovery fixture".toCharArray();
        assertThrows(IllegalArgumentException.class, () -> ResetAdminPassword.reset(connection, 1, "owner", password, password));
        verify(connection, never()).setAutoCommit(anyBoolean());
    }
}
